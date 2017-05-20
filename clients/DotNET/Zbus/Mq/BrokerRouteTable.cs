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

        public void UpdateServer(ServerInfo serverInfo)
        {

        }
        public void RemoveServer(ServerAddress serverAddress)
        {

        }

        public void UpdateVotes(TrackerInfo trackerInfo)
        {

        }
    }
}
