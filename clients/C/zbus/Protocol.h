#ifndef __ZBUS_PROTOCOL_H__
#define __ZBUS_PROTOCOL_H__  

#include "Platform.h"
#include <string>
#include <map>
#include <vector>
using namespace std;


#define HEADER_VERSION_VALUE "0.8.0"  //start from 0.8.0 

//=============================[1] Command Values================================================
//MQ Produce/Consume
#define HEADER_PRODUCE "produce"
#define HEADER_CONSUME "consume"
#define HEADER_ROUTE  "route"     //route back message to sender, designed for RPC 
#define HEADER_RPC  "rpc"       //the same as produce command except rpc set ack false by default

//Topic control
#define HEADER_DECLARE  "declare"
#define HEADER_QUERY  "query"
#define HEADER_REMOVE  "remove"
#define HEADER_EMPTY  "empty"

//High Availability (HA) 
#define HEADER_TRACK_PUB  "track_pub"
#define HEADER_TRACK_SUB  "track_sub"
#define HEADER_TRACKER  "tracker"
#define HEADER_SERVER  "server"


//=============================[2] Parameter Values================================================
#define HEADER_CMD  "cmd"
#define HEADER_TOPIC  "topic"
#define HEADER_TOPIC_MASK  "topic_mask"
#define HEADER_TAG  "tag"
#define HEADER_OFFSET  "offset"

#define HEADER_CONSUME_GROUP  "consume_group"
#define HEADER_GROUP_START_COPY  "group_start_copy"
#define HEADER_GROUP_START_OFFSET  "group_start_offset"
#define HEADER_GROUP_START_MSGID  "group_start_msgid"
#define HEADER_GROUP_START_TIME  "group_start_time"
#define HEADER_GROUP_FILTER  "group_filter"
#define HEADER_GROUP_MASK  "group_mask"
#define HEADER_CONSUME_WINDOW  "consume_window"

#define HEADER_SENDER  "sender"
#define HEADER_RECVER  "recver"
#define HEADER_ID  "id"

#define HEADER_HOST  "host"
#define HEADER_ACK  "ack"
#define HEADER_ENCODING  "encoding"

#define HEADER_ORIGIN_ID  "origin_id"
#define HEADER_ORIGIN_URL  "origin_url"
#define HEADER_ORIGIN_STATUS  "origin_status"

//Security 
#define HEADER_TOKEN "token"

#define HEADER_MASK_PAUSE           (1 << 0)
#define HEADER_MASK_RPC             (1 << 1)
#define HEADER_MASK_EXCLUSIVE       (1 << 2)
#define HEADER_MASK_DELETE_ON_EXIT  (1 << 3)



class ZBUS_API ServerAddress {
public:
	string address;
	bool sslEnabled;
	ServerAddress() {

	}
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


class ZBUS_API ErrorInfo {  //used only for batch operation indication
public:
	exception error;
};

class ZBUS_API TrackItem : ErrorInfo {
public:
	TrackItem() {}
	ServerAddress serverAddress;
	string serverVersion;
};

class ZBUS_API ConsumeGroupInfo : ErrorInfo {
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

class ZBUS_API TopicInfo : TrackItem {
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

class ZBUS_API ServerInfo : TrackItem {
public:
	string infoVersion;
	map<string, TopicInfo> topicTable;
};

class ZBUS_API TrackerInfo : TrackItem {
public:
	string infoVersion;
	map<string, ServerInfo> serverTable;
};  

#endif