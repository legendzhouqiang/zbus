#ifndef __ZBUS_BROKER_H__
#define __ZBUS_BROKER_H__  
 
#include "MqClient.h" 
#include "Kit.h"
#include <map>
#include <string>   
#include <vector>
 
class Vote {
public:
	int64_t version;
	std::vector<ServerAddress> servers;
};

class ZBUS_API BrokerRouteTable {
public:
	std::map<string, TopicInfo> topicTable; 
	std::map<ServerAddress, ServerInfo> serverTable;
	std::map<ServerAddress, Vote> votesTable; 

	void updateTracker(TrackerInfo& trackerInfo) {

	}

	void removeTracker(ServerAddress& trackerAddress) {

	} 
};

class ZBUS_API Broker {
public:
	Broker(std::string trackerAddress, bool sslEnabled = false, std::string sslCertFile = "") {
		ServerAddress serverAddress(trackerAddress, sslEnabled);
		addTracker(serverAddress, sslCertFile);
	}

	virtual ~Broker() {
		for (auto& kv : poolTable) {
			delete kv.second;
		}
		poolTable.clear();

		for (auto& kv : trackerSubscribers) {
			delete kv.second;
		}
		trackerSubscribers.clear();
	}

	void addTracker(ServerAddress& serverAddress, std::string sslCertFile = "") {
		if (trackerSubscribers.count(serverAddress)) {
			return;
		}
		if (sslCertFile != "") {
			sslCertFileTable[serverAddress.address] = sslCertFile;
		}
		MqClient* client = new MqClient(serverAddress.address, serverAddress.sslEnabled, sslCertFile);
		trackerSubscribers[serverAddress] = client;

		client->onConnected = [](MessageClient* client) {
			Message msg;
			msg.setCmd(PROTOCOL_TRACK_SUB);
			client->send(msg);
		};

		client->onMessage = [](Message* msg) {
			msg->print();
			TrackerInfo info;
			parseTrackerInfo(info, *msg);
			


			delete msg;
		}; 
		client->start();
	}

private:



private: 
	BrokerRouteTable routeTable;

	std::map<ServerAddress, MqClientPool*> poolTable;
	std::map<ServerAddress, MqClient*> trackerSubscribers;
	std::map<std::string, std::string> sslCertFileTable;
};

#endif