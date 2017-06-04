#include "Protocol.h"
#include "MessageClient.h"
#include "Buffer.h"
#include "Logger.h"
 
#include<iostream> 
using namespace std;


int main(int argc, char* argv[]) { 
	ServerAddress addr("localhost:15555");  
	MessageClient client(addr); 
	client.connect();
	
	Message msg;
	msg.setCmd("tracker"); 
	msg.print();

	client.send(msg); 
	client.recv();  


	cout << endl;
	system("pause");
}