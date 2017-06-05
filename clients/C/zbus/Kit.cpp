#include "Kit.h"
#include "Protocol.h"

#include <json/value.h> 
#include <json/reader.h>

static void parseServerAddress(ServerAddress& info, Json::Value& root) {
	info.address = root["address"].asString();
	info.sslEnabled = root["sslEnabled"].asBool();
}

static void parseConsumeGroupInfo(ConsumeGroupInfo& info, Json::Value& root){  
	info.consumerCount = root["consumerCount"].asInt();

	Json::Value& consumerList = root["consumerList"];
	for (Json::ValueIterator it = consumerList.begin(); it != consumerList.end(); ++it){
		const Json::Value& value = *it; 
		info.consumerList.push_back(value.asString());
	} 
	info.createdTime = root["createdTime"].asLargestInt();
	info.creator = root["creator"].asString();
	info.filter = root["filter"].asString();
	info.groupName = root["groupName"].asString();
	info.lastUpdatedTime = root["lastUpdatedTime"].asLargestInt();
	info.mask = root["mask"].asInt();
	info.messageCount = root["messageCount"].asLargestInt();
	info.topicName = root["topicName"].asString(); 
}

static void parseTopicInfo(TopicInfo& info, Json::Value& root) { 
	Json::Value& groupList = root["consumeGroupList"];
	for (Json::ValueIterator it = groupList.begin(); it != groupList.end(); ++it) {
		Json::Value& value = *it;
		ConsumeGroupInfo groupInfo;
		parseConsumeGroupInfo(groupInfo, value);
		info.consumeGroupList.push_back(groupInfo);
	} 
	info.consumerCount = root["consumerCount"].asInt();
	info.createdTime = root["createdTime"].asLargestInt();
	info.creator = root["creator"].asString();
	info.lastUpdatedTime = root["lastUpdatedTime"].asLargestInt();
	info.mask = root["mask"].asInt();
	info.messageDepth = root["messageDepth"].asLargestInt(); 
	parseServerAddress(info.serverAddress, root["serverAddress"]);
	info.serverVersion = root["serverVersion"].asString();
	info.topicName = root["topicName"].asString();  
}

static void parseServerInfo(ServerInfo& info, Json::Value& root) {
	info.infoVersion = root["infoVersion"].asString();
	parseServerAddress(info.serverAddress, root["serverAddress"]);
	info.serverVersion = root["serverVersion"].asString();  

	Json::Value& topicTableValue = root["topicTable"];
	vector<string>& topicNames = topicTableValue.getMemberNames();
	for (int i = 0; i < topicNames.size();i++) {
		string& topicName = topicNames[i];
		Json::Value& topicInfoValue = topicTableValue[topicName];
		TopicInfo topicInfo;
		parseTopicInfo(topicInfo, topicInfoValue);
		info.topicTable[topicName] = topicInfo;
	} 
}

bool parseBase(ErrorInfo& info, Json::Value& root, Message& msg) {
	string bodyString = msg.getBodyString();
	if (msg.status != "200") {
		info.isError = true;
		info.error = MqException(bodyString);
		return false;
	} 
	Json::Reader reader;
	reader.parse(bodyString, root); 
	return true;
}

void parseConsumeGroupInfo(ConsumeGroupInfo& info, Message& msg) { 
	Json::Value root;
	if (!parseBase(info, root, msg)) return;

	parseConsumeGroupInfo(info, root); 
}

void parseTopicInfo(TopicInfo& info, Message& msg) {
	Json::Value root;
	if (!parseBase(info, root, msg)) return;

	parseTopicInfo(info, root);
}
void parseServerInfo(ServerInfo& info, Message& msg) {
	Json::Value root;
	if (!parseBase(info, root, msg)) return;

	parseServerInfo(info, root);
}

void parseTrackerInfo(TrackerInfo& info, Message& msg) {
	Json::Value root;
	if (!parseBase(info, root, msg)) return;

	info.infoVersion = root["infoVersion"].asString();
	parseServerAddress(info.serverAddress, root["serverAddress"]); 

	Json::Value& serverTableValue = root["serverTable"];
	vector<string>& serverAddressList = serverTableValue.getMemberNames();
	for (int i = 0; i < serverAddressList.size(); i++) {
		string& serverAddress = serverAddressList[i];
		Json::Value& serverInfoValue = serverTableValue[serverAddress];
		ServerInfo serverInfo;
		parseServerInfo(serverInfo, serverInfoValue);
		info.serverTable[serverAddress] = serverInfo;
	}

	info.serverVersion = root["serverVersion"].asString(); 
}