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

        public bool CanRemove(ISet<ServerAddress> votedTrackerSet)
        {
            return votedTrackerSet.Count < 1;
        }

        public IList<ServerAddress> UpdateVotes(TrackerInfo trackerInfo)
        {
            ServerAddress trackerAddress = trackerInfo.ServerAddress;
            var localVotesTable = new ConcurrentDictionary<ServerAddress, HashSet<ServerAddress>>(VotesTable);
            var trackedServerSet = new HashSet<ServerAddress>(trackerInfo.TrackedServerList);
            foreach(var serverAddress in trackedServerSet)
            {
                var votedTracker = new HashSet<ServerAddress>();
                if (localVotesTable.ContainsKey(serverAddress))
                {
                    votedTracker = localVotesTable[serverAddress];
                }
                votedTracker.Add(trackerAddress);
            } 
            var toRemove = new List<ServerAddress>();
            foreach(var kv in localVotesTable)
            {
                ServerAddress serverAddress = kv.Key;
                HashSet<ServerAddress> votedTrackers = kv.Value;
                if(votedTrackers.Contains(trackerAddress) && !trackedServerSet.Contains(serverAddress))
                {
                    votedTrackers.Remove(trackerAddress);
                }
                if (CanRemove(votedTrackers))
                {
                    toRemove.Add(serverAddress);
                }
            }
            this.VotesTable = localVotesTable;
            if(toRemove.Count == 0)
            {
                return toRemove;
            }

            foreach(ServerAddress serverAddress in toRemove)
            {
                if (ServerTable.ContainsKey(serverAddress))
                {
                    ServerTable.Remove(serverAddress);
                } 
            }
            this.RebuildTopicTable(); 
            return toRemove;
        }

        public void UpdateServer(ServerInfo serverInfo)
        {
            ServerTable[serverInfo.ServerAddress] = serverInfo;
            RebuildTopicTable();
        }

        public void RemoveServer(ServerAddress serverAddress)
        {
            if (ServerTable.ContainsKey(serverAddress))
            {
                ServerTable.Remove(serverAddress);
            }
            RebuildTopicTable(); 
        } 

        private void RebuildTopicTable()
        {
            var localTopicTable = new ConcurrentDictionary<string, IList<TopicInfo>>();
            foreach(var kv in ServerTable)
            {
                ServerInfo serverInfo = kv.Value;
                foreach(var tkv in serverInfo.TopicTable)
                {
                    TopicInfo topicInfo = tkv.Value;
                    IList<TopicInfo> topicServerList = null;
                    if (!localTopicTable.ContainsKey(topicInfo.TopicName))
                    {
                        topicServerList = new List<TopicInfo>();
                        localTopicTable[topicInfo.TopicName] = topicServerList;
                    } 
                    else
                    {
                        topicServerList = localTopicTable[topicInfo.TopicName];
                    }
                    topicServerList.Add(topicInfo);
                }
            }
            this.TopicTable = localTopicTable;
        }
    }
}
