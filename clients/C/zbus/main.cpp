#include "Protocol.h"
#include "MessageClient.h"
 
#include<iostream>

using namespace std;


int main(int argc, char* argv[]) { 
	ServerAddress addr("localhost:15555"); 
	Message msg; 
	MessageClient client(addr);
	
	int rc = client.connect();
	client.send(msg);
	client.recv(); 
	
	system("pause");
}