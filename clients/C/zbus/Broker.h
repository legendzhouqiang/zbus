#ifndef __ZBUS_BROKER_H__
#define __ZBUS_BROKER_H__  
 
#include "MqClient.h" 
#include <map>
#include <string>   
 
class ZBUS_API BrokerRouteTable {
public:

};

class ZBUS_API Broker {
public:
	Broker(std::string trackerAddress, bool sslEnabled = false, std::string sslCertFile = "") {

	}
	virtual ~Broker() {
		for (auto& kv : poolTable) {
			delete kv.second;
		}
		poolTable.clear();
	}


private: 
	std::map<std::string, MqClientPool*> poolTable;
	BrokerRouteTable routeTable;
	std::map<std::string, std::string> sslCertFileTable;
};

#endif