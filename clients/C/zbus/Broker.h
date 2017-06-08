#ifndef __ZBUS_BROKER_H__
#define __ZBUS_BROKER_H__  
 
#include "MqClient.h" 
#include "Kit.h"
#include <map>
#include <string>   
#include <vector>
#include <algorithm>

#include <iostream>

 
class Vote {
public:
	int64_t version = 0;
	std::vector<ServerAddress> servers;
};

class ZBUS_API BrokerRouteTable {
public:
	std::map<string, vector<TopicInfo>> topicTable; 
	std::map<ServerAddress, ServerInfo> serverTable;
	std::map<ServerAddress, Vote> votesTable; 


	BrokerRouteTable(double voteFactor = 0.5): voteFactor(voteFactor) { 
	}

	vector<ServerAddress> updateTracker(TrackerInfo& trackerInfo) { 
		//1) update votes
		ServerAddress& trackerAddress = trackerInfo.serverAddress; 
		int64_t trackerVersion = trackerInfo.infoVersion;
		Vote vote;
		if (votesTable.count(trackerAddress)) {
			vote = votesTable[trackerAddress];
		}
		if (trackerVersion <= vote.version) {
			return vector<ServerAddress>();
		}

		vote.version = trackerVersion;
		vector<ServerAddress> servers;
		for (auto& kv : trackerInfo.serverTable) {
			servers.push_back(kv.second.serverAddress);
		}
		vote.servers = servers;
		votesTable[trackerAddress] = vote; 

		//2) merge server table 
		for (auto& kv : trackerInfo.serverTable) {
			ServerInfo& serverInfo = kv.second;
			if (serverTable.count(serverInfo.serverAddress) > 0) {
				ServerInfo& oldServerInfo = serverTable[serverInfo.serverAddress];
				if (oldServerInfo.infoVersion >= serverInfo.infoVersion) {
					continue;
				}
			}
			serverTable[serverInfo.serverAddress] = serverInfo;
		}
		//3) purge
		return purge();
	} 


	vector<ServerAddress> removeTracker(ServerAddress& trackerAddress) {
		votesTable.erase(trackerAddress);
		return purge();
	} 

private:
	double voteFactor = 0.5;
	vector<ServerAddress> purge() {
		vector<ServerAddress> toRemove;
		for (auto& s : serverTable) {
			ServerInfo& serverInfo = s.second;
			ServerAddress& serverAddress = serverInfo.serverAddress;
			int count = 0;
			for (auto& v : votesTable) {
				Vote& vote = v.second; 
				vector<ServerAddress>& servers = vote.servers;
				if (std::find(servers.begin(), servers.end(), serverAddress) != servers.end()) {
					count++;
				}
			}
			if (count < votesTable.size()*voteFactor) {
				toRemove.push_back(serverAddress);
			}
		}

		std::map<ServerAddress, ServerInfo> serverTableLocal = this->serverTable;
		for (ServerAddress& addr : toRemove) {
			serverTableLocal.erase(addr);
		}

		std::map<string, vector<TopicInfo>> topicTableLocal;
		for (auto& s : serverTableLocal) {
			ServerInfo& serverInfo = s.second;
			for (auto& t : serverInfo.topicTable) {
				TopicInfo& topicInfo = t.second;
				string& topicName = topicInfo.topicName;
				if (topicTableLocal.count(topicName) < 1) {
					topicTableLocal[topicName] = vector<TopicInfo>();
				}
				topicTableLocal[topicName].push_back(topicInfo);
			} 
		}

		this->serverTable = serverTableLocal; 
		this->topicTable = topicTableLocal;
		return toRemove;
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

		client->contextObject = &this->routeTable;   //Ugly!!!, any better way to access routeTable?
		client->onMessage = [](Message* msg, void* ctx) {  
			TrackerInfo info;
			parseTrackerInfo(info, *msg); 

			BrokerRouteTable* routeTable = (BrokerRouteTable*)ctx;
			routeTable->updateTracker(info); 

			delete msg;
		};

		client->start();
	}  


private: 
	BrokerRouteTable routeTable;

	std::map<ServerAddress, MqClientPool*> poolTable;
	std::map<ServerAddress, MqClient*> trackerSubscribers;
	std::map<std::string, std::string> sslCertFileTable;
};

#endif