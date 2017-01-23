#The HA Model (Remove of SPOF)  
Yet, Data Replication Not Support.

**HA Diagram**
	                    
	                    [Tracker1]   [Tracker2]
			                |           |
		    +---------------+-----+-----+---------------+
			|                     ^                     |
			|sub route            | pub route           | sub route
			|                     |                     v
			v      ------->  [MqServer1]  ---------> Consumer
		 Producer  choose ?                         
				   ------->  [MqServer2]  ---------> Consumer

Pysically, Tracker serves in MqServer.
1. Single Point Of Failure(SPOF) will not affect the availability of the whole system.
2. Sophiscated choosing algorithm configurable to Producer/Consumer to be more Application-awared.


##Tracking Message Flow

		     +-----sub-----> Broker <-----sub-----+
	         |                                    |
	         |                                    |
	Server(TrackService) --outbound pub--> Server(TrackService)
	         ^                                    |
	         |                                    v
	         +----------inbound tracking----------+	 


	TopicInfo
		 + publisher
		 + timestamp
		 + live 
		 + serverAddress 
		 + topicName
		 + consumerGroupList

	ServerInfo
		 + publisher
		 + timestamp
		 + live 
		 + serverAddress 
		 + topicMap: Map<String, TopicInfo>


	TrackService(trackServerList--outbound)
		- { serverAddress=>ServerInfo }
		- allOutbounds
		- healthyOutbounds 
		- healthyInbounds
		- subscribers 
		+ queryServerTable
		+ publish(serverInfo)
		+ pulbish(topiInfo)
		+ subscribe()
		+ start
		+ close

**queryServerTable**
   
	plus current ServerInfo if included


**start**

	connect to all outbound servers
		onConnected: add client to healthyOutbounds 
		onDisconnected: remove from healthyOutbonds


**publish(serverInfo)**

	1)server join
     	onConnected(call serverJoin)
	 	onDisconnected(call serverLeave)
	2)server upate
     	call serverUpdate
    3)server leave 
     	call serverLeave
    
**serverJoin(serverInfo)** 
	
	add to serverMap
	call publish2outbounds
	call publish2subscribers 

**serverUpdate(serverInfo)**
	
	upate serverMap
	call publish2outbounds
	call publish2subscribers 
      
**serverLeave(serverInfo)**
  
	remove from serverMap
	call publish2outbounds
	call publish2subscribers   

**Report timer(every 10sec configurable)**

	call publish2outbounds
	call publish2subscribers   
