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

	ServerInfo queryServer(int timeout=3000) {
		Message msg;
		msg.setCmd(PROTOCOL_QUERY);
		msg.setToken(token); 
		Message* res = invoke(msg, timeout); 

		ServerInfo info;
		parseServerInfo(info, *res); 

		if (res) {
			delete res;
		} 
		return info;
	}  

};

#endif