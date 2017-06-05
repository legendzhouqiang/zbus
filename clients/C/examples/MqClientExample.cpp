#include "MqClient.h"  


int main(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_INFO); 
	Logger* log = Logger::getLogger();

	MqClient client("localhost:15555");
	client.connect();
	 
	TrackerInfo info = client.queryTracker();
	log->info("%s", info.infoVersion.c_str());

	TopicInfo topicInfo = client.queryTopic("hong"); 
	log->info("%s", topicInfo.topicName.c_str());

	ConsumeGroupInfo groupInfo = client.queryGroup("hong", "hong");
	log->info("%s", groupInfo.groupName.c_str());

	system("pause");
	return 0;
}