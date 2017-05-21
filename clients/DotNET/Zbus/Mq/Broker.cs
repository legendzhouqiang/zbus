using log4net;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Zbus.Mq
{
    public class Broker
    { 
        private static readonly ILog log = LogManager.GetLogger(typeof(Broker));

        public int? ClientPoolSize { get; set; }
        public string DefaultSslCertFile { get; set; } 
        public event Action<MqClientPool> ServerJoin;
        public event Action<ServerAddress> ServerLeave;
        public BrokerRouteTable RouteTable { get; private set; }

        private IDictionary<ServerAddress, MqClientPool> poolTable = new ConcurrentDictionary<ServerAddress, MqClientPool>();
        private IDictionary<ServerAddress, MqClient> trackerSubscribers = new ConcurrentDictionary<ServerAddress, MqClient>();
        

        private IDictionary<string, string> sslCertFileTable = new ConcurrentDictionary<string, string>();

        public Broker()
        {
            RouteTable = new BrokerRouteTable();
        }
        public void AddTracker(ServerAddress trackerAddress, string certFile = null, int waitTime=3000)
        {
            MqClient client = new MqClient(trackerAddress, certFile);
            trackerSubscribers[trackerAddress] = client;
            CountdownEvent countDown = new CountdownEvent(1);
            bool firstTime = true;
            client.Connected += async () =>
            {
                Message msg = new Message
                {
                    Cmd = Protocol.TRACK_SUB,
                };
                await client.SendAsync(msg);
            };
            client.MessageReceived += (msg) =>
            { 
                if(msg.Status != "200")
                {
                    log.Error(msg.BodyString);
                    return;
                }
                TrackerInfo trackerInfo = JsonKit.DeserializeObject<TrackerInfo>(msg.BodyString);
                if (firstTime)
                {
                    countDown.AddCount(trackerInfo.TrackedServerList.Count - 1);
                    firstTime = false;
                }
                else
                {
                    countDown = null;
                } 
                
                foreach(ServerAddress serverAddress in trackerInfo.TrackedServerList)
                {
                    AddServer(serverAddress, null, countDown);
                }

                IList<ServerAddress> toRemove = RouteTable.UpdateVotes(trackerInfo); 
                foreach(ServerAddress serverAddress in toRemove)
                {
                    if (poolTable.ContainsKey(serverAddress))
                    {
                        log.Info(serverAddress + ", left");
                        MqClientPool pool = poolTable[serverAddress];
                        poolTable.Remove(serverAddress);
                        try
                        {
                            ServerLeave?.Invoke(serverAddress);
                            pool.Dispose();
                        }
                        catch(Exception e)
                        {
                            log.Error(e);
                        }
                    }
                } 
            };
            client.Start();

            countDown.Wait(waitTime);
            countDown.Dispose(); 
        }
        public void AddTracker(string trackerAddress, string certFile = null)
        {
            AddTracker(new ServerAddress(trackerAddress), certFile);
        }
        public void AddServer(ServerAddress serverAddress, string certFile = null, CountdownEvent countDown = null)
        {
            certFile = GetCertFile(serverAddress, certFile);

            MqClientPool pool = new MqClientPool(serverAddress, certFile);
            if (ClientPoolSize.HasValue)
            {
                pool.MaxCount = ClientPoolSize.Value;
            }

            pool.Connected += (serverInfo) =>
            {
                RouteTable.UpdateServer(serverInfo);
                if(countDown != null)
                {
                    countDown.Signal();
                }
                ServerJoin?.Invoke(pool);
            };
            pool.Disconnected += (remoteServerAddr) =>
            {
                RouteTable.RemoveServer(remoteServerAddr);
                ServerLeave?.Invoke(remoteServerAddr); 

                poolTable.Remove(pool.ServerAddress);
                pool.Dispose(); 
            };
            pool.StartMonitor();
            poolTable[pool.ServerAddress] = pool;
        }

        public void AddServer(string serverAddress, string certFile = null, CountdownEvent countDown = null)
        {
            AddServer(new ServerAddress(serverAddress), certFile);
        }

        private string GetCertFile(ServerAddress serverAddress, string certFile = null)
        {
            if (certFile != null)
            {
                sslCertFileTable[serverAddress.Address] = certFile;
                return certFile;
            }
            if (sslCertFileTable.ContainsKey(serverAddress.Address))
            {
                return sslCertFileTable[serverAddress.Address];
            }
            return DefaultSslCertFile;
        }
    }
}
