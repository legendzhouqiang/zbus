##Broker Design
In the model Producer-Broker-Consumer(PBC), broker acts as the role of picking up real MqServer instance(represented by MqClient connection pool) for Producer and Consumer. However, the algorithm of how to pick up right MqServer is not in the control of Broker, Producer and Consumer provide it. So actually, Broker's main responsibility is to collect the topology info(represented by BrokerRouteTable,including Topic and Server) from many MqServers. To make the MqServer join and leave dynamically, Tracker is usually involved. A tracker tracks MqServers joined, to make data more consistent, a tracker is very simple, embedded in a MqServer instance, just keep the tracked server address list, get notified if any MqServer report necessary updates. The tracker propagates the changes notification to subscribers which usually are the brokers, the broker then obtains the whole MqServer info again directly from each MqServer instance.

In short, Broker needs to select MqServer instance(s) based on the algorithm provided by Producer or Consumer, capable of adding MqServer and Tracker to build BrokerRouteTable.

**1. Dynamic way**

Broker.addTracker(trackerAddress)

**2. Static way**

Broker.addServer(serverAddress)
Broker.removeServer(serverAddress)


**Manage SSL**

Broker.addSslCertFile(address, certFile)


###BrokerRouteTable

**serverTable**

ServerAddress => ServerInfo, serverTable keeps the raw serverInfo collected from each MqServer


**topicTable**

Topic String => List of TopicInfo(same topic on different MqServers), topicTable is dynamically built from serverTable, get updated if serverTable updates notified.

**votesTable**

ServerAddress => Voted Tracker list, MqServer's votes get down to some extent(defined) should be removed.


**BrokerRouteTable.updateServer**

**BrokerRouteTable.removeServer**

**BrokerRouteTable.updateVotes**
