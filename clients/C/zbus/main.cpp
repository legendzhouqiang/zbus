#include "Protocol.h"
#include "MessageClient.h"
#include "Buffer.h"
 
#include<iostream>
#include<map>

using namespace std;


int main(int argc, char* argv[]) { 
	ServerAddress addr("localhost:15555"); 
	Message msg; 
	MessageClient client(addr);

	Buffer buf(1024);
	
	
	msg.setCmd(Protocol::CONSUME);
	msg.setTopic("hong");
	msg.setConsumeGroup("xxx");
	 

	
	system("pause");
}