#ifndef __ZBUS_CONSUMER_H__
#define __ZBUS_CONSUMER_H__  
 
#include "MqAdmin.h" 

typedef void(*MessageHandler)(Message& msg, MqClient* client);
class ConsumeThread {
private:
	MqClientPool* pool;
	int connectionCount;
	
	MessageHandler messageHander;

	std::string topic;
	std::string consumeGroup;
	int consumeWindow = 1;
	int consumeTimeout;
	std::thread* clientThreads;
	std::vector<MqClient*> clients;
public:
	ConsumeThread(MqClientPool* pool, MessageHandler messageHander, int connectionCount = 1, int timeout = 1000) :
		pool(pool), messageHander(messageHander), connectionCount(connectionCount), consumeTimeout(timeout){

		
	}

	~ConsumeThread() {
		if (clientThreads != NULL) {
			delete[] clientThreads;
		}
		for (MqClient* client : clients) {
			delete client;
		}
		clients.clear();
	}

	void start() {
		if (clientThreads != NULL) return;
		clientThreads = new std::thread[this->connectionCount];
		
		for (int i = 0; i < this->connectionCount; i++) {
			MqClient* client = pool->makeClient();
			clients.push_back(client);
			clientThreads[i] = std::thread(&ConsumeThread::run, this, client);
		} 
	}

private:
	Message* take(MqClient* client) {
		Message* res = client->consume(topic, consumeGroup, consumeWindow, consumeTimeout);
		if (res->status == "404") {
			client->declareGroup(topic, consumeGroup);
			delete res;
			return take(client);
		}
		if (res->status == "200") {
			res->setId(res->getOriginId()); 
			res->removeHeader(PROTOCOL_ORIGIN_ID);
			if (res->getOriginUrl() != "") {
				res->url = res->getOriginUrl();
				res->removeHeader(PROTOCOL_ORIGIN_URL);
				res->status = "";
			}
		}
		return res;
	}

	void run(MqClient* client) {

	}

};
 

class ZBUS_API Consumer : public MqAdmin {
protected:
	ServerSelector consumeSelector;
public:
	Consumer(Broker* broker) : MqAdmin(broker) {
		consumeSelector = [](BrokerRouteTable& routeTable, Message& msg) {
			std::vector<ServerAddress> res;
			for (auto& kv : routeTable.serverTable) {
				res.push_back(kv.second.serverAddress);
			}
			return res;
		};
	}
};
  
#endif