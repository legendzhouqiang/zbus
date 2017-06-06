#include "Broker.h"  


int main_broker(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_DEBUG); 
	Logger* log = Logger::getLogger();
	 
	ServerAddress address1("localhost:15555");
	ServerAddress address2 = address1;

	vector<ServerAddress> addressList;
	addressList.push_back(address1);
	addressList.push_back(address2);


	std::map<ServerAddress, string> table;
	table[ServerAddress("localhost:15555")] = "hong";
	table[ServerAddress("localhost:15555", true)] = "hong2";

	Broker broker("localhost:15555");

	system("pause");
	return 0;
}