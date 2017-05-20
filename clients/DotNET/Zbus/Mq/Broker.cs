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

        private IDictionary<ServerAddress, MqClientPool> poolTable = new ConcurrentDictionary<ServerAddress, MqClientPool>();
        private BrokerRouteTable routeTable = new BrokerRouteTable();
        private IDictionary<string, string> sslCertFileTable = new ConcurrentDictionary<string, string>();

        public async Task AddTracker(ServerAddress trackerAddress, string certFile = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.TRACK_SUB,
            };
            await Task.Run(() =>
            {

            });
        }

        public async Task AddServerAsync(ServerAddress serverAddress, string certFile = null, CancellationToken? token=null)
        {
            if (certFile != null)
            {
                sslCertFileTable[serverAddress.Address] = certFile;
            }

            MqClient client = new MqClient(serverAddress, certFile); 
            ServerInfo serverInfo = await client.QueryServerAsync(token);
            routeTable.UpdateServer(serverInfo);
        }

        public async Task AddServerAsync(string serverAddress, string certFile = null, CancellationToken? token = null)
        {
            await AddServerAsync(new ServerAddress(serverAddress), certFile, token);
        }
    }
}
