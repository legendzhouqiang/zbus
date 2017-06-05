#ifndef __ZBUS_MQ_CLIENT_H__
#define __ZBUS_MQ_CLIENT_H__  
 
#include "MessageClient.h"
 
ZBUS_API class MqClient : public MessageClient {
public:
	MqClient(string address, bool sslEnabled = false, string sslCertFile = "") :
		MessageClient(address, sslEnabled, sslCertFile){

	}
	virtual ~MqClient() {

	}



};

#endif