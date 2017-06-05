#ifndef __ZBUS_KIT_H__
#define __ZBUS_KIT_H__   
   
#include "Message.h"
#include "Protocol.h" 
#include <string>
using namespace std;

void parseConsumeGroupInfo(ConsumeGroupInfo& info, Message& msg);
void parseTopicInfo(TopicInfo& info, Message& msg);
void parseServerInfo(ServerInfo& info, Message& msg);
void parseTrackerInfo(TrackerInfo& info, Message& msg);


#endif