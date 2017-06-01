#ifndef __ZBUS_MESSAGE_CLIENT_H__
#define __ZBUS_MESSAGE_CLIENT_H__  

#include "Protocol.h"

class MessageClient {
private:
	int _fd;
	ServerAddress serverAddress;
	string sslCertFile;

public:
	int connect(char* serverAddress, char* sslCertFile = NULL) {
		return 0;
	}

};

#endif