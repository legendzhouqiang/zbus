#include "Protocol.h"
#include "MessageClient.h"
#include "Buffer.h"
#include "Logger.h"
 
#include<iostream> 
using namespace std;


int main(int argc, char* argv[]) { 
	Logger::configDefaultLogger(0, LOG_DEBUG);
	ServerAddress addr("localhost:15555");  
	MessageClient client(addr); 
	client.connect();
	
	Message msg;
	msg.setCmd("tracker");  
	 

	stringstream ss("GET /tracker HTTP/1.1\r\ncmd:produce\r\n\r\n");
	string line;
	getline(ss, line, '\n'); 
	cout << line << endl;

	system("pause");
}