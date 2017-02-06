#The HA Model (Remove of SPOF)  
Yet, Data Replication Not Support.

**HA Diagram**
	                    
	                    [Tracker1]   [Tracker2]
	                        |           |
	        +---------------+-----+-----+---------------+
	        |                     ^                     |
	        |sub(to_update)+pull  | pub(to_update)      | sub(to_update)+pull
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
	Server(TrackService) --pub to tracker--> Server(TrackService)
	         ^                                    |
	         |                                    v
	         +--------server online check---------+	 
	
	TrackerInfo[TrackItem] 
		- liveServerList: List<String>

	ServerInfo[TrackItem]  
		- trackerList List<String>
		- topicMap: Map<String, TopicInfo>

	TopicInfo[TrackItem]  
		- topicName: String
		- consumerGroupList: List<ConsumerGroupInfo>  
	
	TrackItem
		- serverAddress: String 
		- serverVersion: String
