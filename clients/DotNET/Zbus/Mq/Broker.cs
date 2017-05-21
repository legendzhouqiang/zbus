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

        private IDictionary<ServerAddress, MqClientPool> poolTable = new ConcurrentDictionary<ServerAddress, MqClientPool>();
        private IDictionary<ServerAddress, MqClient> trackerSubscribers = new ConcurrentDictionary<ServerAddress, MqClient>();
        private BrokerRouteTable routeTable = new BrokerRouteTable();

        private IDictionary<string, string> sslCertFileTable = new ConcurrentDictionary<string, string>();

        public void AddTracker(ServerAddress trackerAddress, string certFile = null)
        {
            MqClient client = new MqClient(trackerAddress, certFile);
            trackerSubscribers[trackerAddress] = client;
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
                foreach(ServerAddress serverAddress in trackerInfo.TrackedServerList)
                {
                    AddServer(serverAddress);
                }

                IList<ServerAddress> toRemove = routeTable.UpdateVotes(trackerInfo); 
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
        }
        public void AddTracker(string trackerAddress, string certFile = null)
        {
            AddTracker(new ServerAddress(trackerAddress), certFile);
        }
        public void AddServer(ServerAddress serverAddress, string certFile = null)
        {
            certFile = GetCertFile(serverAddress, certFile);

            MqClientPool pool = new MqClientPool(serverAddress, certFile);
            if (ClientPoolSize.HasValue)
            {
                pool.MaxCount = ClientPoolSize.Value;
            }

            pool.Connected += (serverInfo) =>
            {
                routeTable.UpdateServer(serverInfo);

                ServerJoin?.Invoke(pool);
            };
            pool.Disconnected += (remoteServerAddr) =>
            {
                routeTable.RemoveServer(remoteServerAddr);
                ServerLeave?.Invoke(remoteServerAddr); 

                poolTable.Remove(pool.ServerAddress);
                pool.Dispose(); 
            };
            pool.StartMonitor();
            poolTable[pool.ServerAddress] = pool;
        }

        public void AddServer(string serverAddress, string certFile = null)
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
