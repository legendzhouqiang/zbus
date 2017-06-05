#include "MqClient.h" 


int main(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_INFO); 
	Logger* log = Logger::getLogger();

	MqClient client("localhost:15555");
	client.connect();
	 
	ServerInfo info = client.queryServer();
	log->info("%s", info.infoVersion.c_str());

	system("pause");
	return 0;
}