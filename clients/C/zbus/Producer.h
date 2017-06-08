#ifndef __ZBUS_PRODUCER_H__
#define __ZBUS_PRODUCER_H__  
 
#include "MqAdmin.h" 

  
class ZBUS_API Producer : public MqAdmin {
protected:
	ServerSelector produceSelector;
public:
	Producer(Broker* broker) : MqAdmin(broker) {
		produceSelector = [](BrokerRouteTable& routeTable, Message& msg) {
			std::vector<ServerAddress> res;
			string topic = msg.getTopic();
			if (topic == "") {
				throw MqException("Message missing topic");
			}
			if (routeTable.topicTable.count(topic) < 1) {
				return res;
			}
			vector<TopicInfo>& topicServerList = routeTable.topicTable[topic];
			if (topicServerList.size() < 1) return res;
			TopicInfo& target = topicServerList[0];
			for (TopicInfo& current : topicServerList) {
				if (current.consumerCount > target.consumerCount) {
					target = current;
				}
			}  
			res.push_back(target.serverAddress);
			return res;
		};
	} 

	void produce(Message& msg, int timeout = 3000, ServerSelector selector = NULL) {
		msg.setCmd(PROTOCOL_PRODUCE); 
		if (selector == NULL) {
			selector = this->produceSelector; 
		}
		vector<MqClientPool*> pools = broker->select(selector, msg);  
		if (pools.size() < 0) throw new MqException("Missing MqServer for topic: " + msg.getTopic());
		MqClientPool* pool = pools[0];
		
		MqClient* client = NULL;
		try {
			client = pool->borrowClient();
			client->produce(msg, timeout);
		}
		catch (MqException& e) { 
			pool->returnClient(client);
			throw e;
		}
		pool->returnClient(client); 
	}  
};
  
#endif