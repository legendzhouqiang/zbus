#ifndef __ZBUS_MQ_CLIENT_H__
#define __ZBUS_MQ_CLIENT_H__  
 
#include "MessageClient.h"
#include <json/value.h> 
#include <json/reader.h>
#include <iostream>
using namespace std;
 
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
		msg.setCmd(PROTOCOL_QUERY);
		msg.setToken(token); 
		Message* res = invoke(msg, timeout);
		 
		string body = res->getBodyString();
		cout << body << endl;
		Json::Value root;
		Json::Reader reader; 
		reader.parse(body, root);
		

		ServerInfo info;
		info.infoVersion = root["infoVersion"].asString();
		if (res) {
			delete res;
		} 
		return info;
	}




};

#endif