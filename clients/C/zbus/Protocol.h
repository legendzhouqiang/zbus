#ifndef __ZBUS_PROTOCOL_H__
#define __ZBUS_PROTOCOL_H__  

 
#include <string>
#include <map>
#include <vector>
using namespace std;

class Protocol {
public:
	static const string VERSION_VALUE; 

	static const string PRODUCE;
	static const string CONSUME;
	static const string ROUTE;
	static const string RPC;
	static const string DECLARE;
	static const string QUERY;
	static const string REMOVE;
	static const string EMPTY;

	static const string TRACK_PUB;
	static const string TRACK_SUB;
	static const string TRACKER;
	static const string SERVER;

	static const string CMD;
	static const string TOPIC;
	static const string TOPIC_MASK;
	static const string TAG;
	static const string OFFSET;

	static const string CONSUME_GROUP;
	static const string GROUP_START_COPY;
	static const string GROUP_START_OFFSET;
	static const string GROUP_START_MSGID;
	static const string GROUP_START_TIME;
	static const string GROUP_FILTER;
	static const string GROUP_MASK;
	static const string CONSUME_WINDOW;

	static const string SENDER;
	static const string RECVER;
	static const string ID;

	static const string HOST;
	static const string ACK;
	static const string ENCODING;

	static const string ORIGIN_ID;
	static const string ORIGIN_URL;
	static const string ORIGIN_STATUS;

	//Security 
	static const string TOKEN;

	static const int MASK_PAUSE;
	static const int MASK_RPC;
	static const int MASK_EXCLUSIVE;
	static const int MASK_DELETE_ON_EXIT;
}; 

class ServerAddress {
public:
	string address;
	bool sslEnabled;

	ServerAddress(char* address, bool sslEnabled = false) {
		this->address = address;
		this->sslEnabled = sslEnabled;
	}

	ServerAddress(string& address, bool sslEnabled = false) {
		this->address = address;
		this->sslEnabled = sslEnabled;
	}

	ServerAddress(ServerAddress* serverAddress) {
		this->address = serverAddress->address;
		this->sslEnabled = serverAddress->sslEnabled;
	}  
};


class ErrorInfo {  //used only for batch operation indication
public:
	exception error;
};

class TrackItem : ErrorInfo {
public:
	ServerAddress serverAddress;
	string serverVersion; 
};

class ConsumeGroupInfo : ErrorInfo {
public:
	string topicName;
	string groupName;
	int mask;
	string filter;
	int64_t messageCount;
	int consumerCount;
	vector<string> consumerList;

	string creator;
	int64_t createdTime;
	int64_t lastUpdatedTime;
};

class TopicInfo : TrackItem {
public:
	string topicName;
	int mask; 
	int64_t messageDepth; 
	int consumerCount; 
	vector<ConsumeGroupInfo> consumeGroupList; 

	string creator;
	int64_t createdTime;
	int64_t lastUpdatedTime;
};

class ServerInfo : TrackItem {
public:
	string infoVersion;
	map<string, TopicInfo> topicTable;
};

class TrackerInfo : TrackItem {
public:
	string infoVersion;
	map<string, ServerInfo> serverTable;
};


class HttpStatus {
public:
	static map<string, string> Table; 
private:
	static map<string, string> createTable(); 
};



#endif