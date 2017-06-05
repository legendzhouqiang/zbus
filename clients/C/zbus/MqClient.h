#ifndef __ZBUS_MQ_CLIENT_H__
#define __ZBUS_MQ_CLIENT_H__  
 
#include "MessageClient.h"
#include <json/value.h> 
#include <json/reader.h>
 
class ZBUS_API MqClient : public MessageClient {

public:
	string token;

	MqClient(string address, bool sslEnabled = false, string sslCertFile = "") :
		MessageClient(address, sslEnabled, sslCertFile){

	}
	virtual ~MqClient() {

	}

	ServerInfo queryServer(int timeout=3000) {
		Message msg;
		msg.setCmd(PROTOCOL_SERVER);
		msg.setToken(token); 
		Message* res = invoke(msg, timeout);
		
		res->getBodyString();

		Json::Value root;
		Json::Reader reader;
		char* begin = res->getBodyString();
		char* end = begin + res->getBodyLength();
		reader.parse(begin, end, root);
		

		ServerInfo info;
		info.infoVersion = root["infoVersion"].asString();
		if (res) {
			delete res;
		} 
		return info;
	}




};

#endif