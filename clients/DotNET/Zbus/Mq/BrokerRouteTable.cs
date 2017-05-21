using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zbus.Mq
{
    public class BrokerRouteTable
    {
        public IDictionary<ServerAddress, HashSet<ServerAddress>> VotesTable { get; private set; }
        public IDictionary<string, IList<TopicInfo>> TopicTable { get; private set; }
        public IDictionary<ServerAddress, ServerInfo> ServerTable { get; private set; }

        public BrokerRouteTable()
        {
            VotesTable = new ConcurrentDictionary<ServerAddress, HashSet<ServerAddress>>();
            TopicTable = new ConcurrentDictionary<string, IList<TopicInfo>>();
            ServerTable = new ConcurrentDictionary<ServerAddress, ServerInfo>();
        }

        public IList<ServerAddress> UpdateVotes(TrackerInfo trackerInfo)
        {
            Console.WriteLine("TrackerUpdated:" + JsonKit.SerializeObject(trackerInfo));
            return new List<ServerAddress>();
        }

        public void UpdateServer(ServerInfo serverInfo)
        {
            ServerTable[serverInfo.ServerAddress] = serverInfo; 
            Console.WriteLine("Updated:" + JsonKit.SerializeObject(serverInfo));
        }

        public void RemoveServer(ServerAddress serverAddress)
        {
            if (ServerTable.ContainsKey(serverAddress))
            {
                ServerTable.Remove(serverAddress);
            }

            Console.WriteLine("Removed:" + JsonKit.SerializeObject(serverAddress));
        } 
    }
}
