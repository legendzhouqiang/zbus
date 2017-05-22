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
    public delegate ServerAddress[] ServerSelector(BrokerRouteTable routeTable, Message msg);
    public class Broker
    { 
        private static readonly ILog log = LogManager.GetLogger(typeof(Broker));

        public int? ClientPoolSize { get; set; }
        public string DefaultSslCertFile { get; set; } 
        public event Action<MqClientPool> ServerJoin;
        public event Action<ServerAddress> ServerLeave;
        public BrokerRouteTable RouteTable { get; private set; }   
        public IDictionary<ServerAddress, MqClientPool> PoolTable { get; private set; }

        private IDictionary<ServerAddress, MqClient> trackerSubscribers = new ConcurrentDictionary<ServerAddress, MqClient>();
        

        private IDictionary<string, string> sslCertFileTable = new ConcurrentDictionary<string, string>();

        public Broker()
        {
            RouteTable = new BrokerRouteTable();
            PoolTable = new ConcurrentDictionary<ServerAddress, MqClientPool>();
        }


        public MqClientPool[] Select(ServerSelector selector, Message msg)
        {
            ServerAddress[] addressArray = selector(RouteTable, msg);
            if (addressArray == null) return new MqClientPool[0];

            MqClientPool[] res = new MqClientPool[addressArray.Length];
            bool shrink = false;
            for(int i=0;i < addressArray.Length; i++)
            {
                ServerAddress address = addressArray[i];
                if (PoolTable.ContainsKey(address))
                {
                    res[i] = PoolTable[address];
                } 
                else
                {
                    res[i] = null;
                    shrink = true;
                }
            }
            if (!shrink) return res;

            return res.Where<MqClientPool>(e => e != null).ToArray<MqClientPool>(); 
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
                    int addCount = trackerInfo.TrackedServerList.Count - 1;
                    if (addCount > 0)
                    {
                        countDown.AddCount();
                    } 
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
                    if (PoolTable.ContainsKey(serverAddress))
                    {
                        log.Info(serverAddress + ", left");
                        MqClientPool pool = PoolTable[serverAddress];
                        PoolTable.Remove(serverAddress);
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

                PoolTable.Remove(pool.ServerAddress);
                pool.Dispose(); 
            };
            pool.StartMonitor();
            PoolTable[pool.ServerAddress] = pool;
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
