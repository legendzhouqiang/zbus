#ifndef __ZBUS_MQ_CLIENT_H__
#define __ZBUS_MQ_CLIENT_H__  
 
#include "MessageClient.h"
#include "Kit.h" 
 
class ZBUS_API ConsumeGroup {
public:
	string groupName;
	string filter;          //filter on message'tag
	int mask = -1;

	string startCopy;       //create group from another group 
	int64_t startOffset = -1;
	string startMsgId;      //create group start from offset, msgId to check valid
	int64_t startTime = -1;    //create group start from time 

	void load(Message& msg) {
		groupName = msg.getConsumeGroup();
		filter = msg.getGroupFilter();
		mask = msg.getGroupMask();
		startCopy = msg.getGroupStartCopy();
		startOffset = msg.getGroupStartOffset();
		startMsgId = msg.getGroupStartMsgId();
		startTime = msg.getGroupStartTime();
	}

	void writeTo(Message& msg) {
		msg.setConsumeGroup(groupName);
		msg.setGroupFilter(filter); 
		msg.setGroupMask(mask);
		msg.setGroupStartCopy(startCopy);
		msg.setGroupStartOffset(startOffset);
		msg.setGroupStartMsgId(startMsgId);
		msg.setGroupStartTime(startTime);
	}
};

class ZBUS_API MqClient : public MessageClient {

public:
	string token;

	MqClient(string address, bool sslEnabled = false, string sslCertFile = "") :
		MessageClient(address, sslEnabled, sslCertFile){

	}
	virtual ~MqClient() { }

	TrackerInfo queryTracker(int timeout = 3000) {
		Message msg;
		msg.setCmd(PROTOCOL_TRACKER);
		msg.setToken(token);
		Message* res = invoke(msg, timeout);

		TrackerInfo info;
		parseTrackerInfo(info, *res);

		if (res) delete res; 
		return info;
	}

	ServerInfo queryServer(int timeout=3000) {
		Message msg;
		msg.setCmd(PROTOCOL_SERVER);
		msg.setToken(token); 
		Message* res = invoke(msg, timeout); 

		ServerInfo info;
		parseServerInfo(info, *res); 

		if (res) delete res; 
		return info;
	}  

	TopicInfo queryTopic(string topic, int timeout = 3000) {
		Message msg;
		msg.setCmd(PROTOCOL_QUERY);
		msg.setTopic(topic);
		msg.setToken(token);
		Message* res = invoke(msg, timeout);

		TopicInfo info;
		parseTopicInfo(info, *res);

		if (res) delete res; 
		return info;
	}

	ConsumeGroupInfo queryGroup(string topic, string group, int timeout = 3000) {
		Message msg;
		msg.setCmd(PROTOCOL_QUERY);
		msg.setTopic(topic);
		msg.setConsumeGroup(group);
		msg.setToken(token);
		Message* res = invoke(msg, timeout);

		ConsumeGroupInfo info;
		parseConsumeGroupInfo(info, *res);

		if (res) delete res; 
		return info;
	} 

	TopicInfo declareTopic(string topic, int topicMask=-1, int timeout = 3000) {
		Message msg;
		msg.setCmd(PROTOCOL_DECLARE);
		msg.setTopic(topic);
		msg.setTopicMask(topicMask);
		msg.setToken(token);
		Message* res = invoke(msg, timeout);

		TopicInfo info;
		parseTopicInfo(info, *res);

		if (res)  delete res; 
		return info;
	}

	ConsumeGroupInfo declareGroup(string topic, ConsumeGroup& group, int timeout = 3000) {
		Message msg;
		msg.setCmd(PROTOCOL_DECLARE);
		msg.setTopic(topic); 
		msg.setToken(token);
		group.writeTo(msg);

		Message* res = invoke(msg, timeout);

		ConsumeGroupInfo info;
		parseConsumeGroupInfo(info, *res);

		if (res)  delete res; 

		return info;
	}

	void removeTopic(string topic, int timeout = 3000) {
		removeGroup(topic, "", timeout);
	}

	void removeGroup(string topic, string group, int timeout = 3000) {
		Message msg;
		msg.setCmd(PROTOCOL_REMOVE);
		msg.setTopic(topic);
		msg.setConsumeGroup(group);
		msg.setToken(token); 

		Message* res = invoke(msg, timeout);

		if (res && res->status != "200") {
			MqException error(res->getBodyString());
			delete res;
			throw error;
		}

		if (res) delete res; 
	}

	void emptyTopic(string topic, int timeout = 3000) {
		emptyGroup(topic, "", timeout);
	}

	void emptyGroup(string topic, string group, int timeout = 3000) {
		Message msg;
		msg.setCmd(PROTOCOL_EMPTY);
		msg.setTopic(topic);
		msg.setConsumeGroup(group);
		msg.setToken(token);

		Message* res = invoke(msg, timeout);

		if (res && res->status != "200") {
			MqException error(res->getBodyString());
			delete res;
			throw error;
		}

		if (res) delete res;
	}  
};

#endif