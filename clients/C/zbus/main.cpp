#include "Protocol.h"
#include "MessageClient.h"
#include "Buffer.h"
 
#include<iostream> 
using namespace std;


int main(int argc, char* argv[]) { 
	ServerAddress addr("localhost:15555"); 
	Message msg; 
	MessageClient client(addr); 
	
	
	msg.setCmd(Protocol::CONSUME);
	msg.setTopic("hong");
	msg.setConsumeGroup("xxx");  
	msg.setBody(string("hello world")); 


	ByteBuffer* buf = msg.encode();
	buf->print();
	delete buf;  

	cout << endl;
	system("pause");
}