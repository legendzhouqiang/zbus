#ifndef __ZBUS_MQ_CLIENT_H__
#define __ZBUS_MQ_CLIENT_H__  
 
#include "MessageClient.h"
#include "Kit.h" 
 
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

		if (res) {
			delete res;
		}
		return info;
	}

	ServerInfo queryServer(int timeout=3000) {
		Message msg;
		msg.setCmd(PROTOCOL_SERVER);
		msg.setToken(token); 
		Message* res = invoke(msg, timeout); 

		ServerInfo info;
		parseServerInfo(info, *res); 

		if (res) {
			delete res;
		} 
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

		if (res) {
			delete res;
		}
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

		if (res) {
			delete res;
		}
		return info;
	}

};

#endif