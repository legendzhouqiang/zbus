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
	msg.setBody("hello world");

	ByteBuffer buf;
	msg.encode(buf);
	buf.flip();

	buf.print();
	Message msg2;
	msg2.decode(buf);

	msg2.print();
	   

	system("pause");
}