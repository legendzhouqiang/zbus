#include "Protocol.h" 
#include "Logger.h"
#include "MessageClient.h"
#include <iostream>
using namespace std;


int main(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_DEBUG);

	MessageClient client("localhost:15555");
	client.connect();

	for (int i = 0; i<100; i++) {
		Message msg;
		msg.setCmd("tracker");
		msg.setBody("hello world");

		client.send(msg);
		string msgid = msg.getId();
		int rc;
		Message* res = client.recv(rc, msgid.c_str());
		if (res) {
			res->print();
			delete res;
		}
	}

	system("pause");
}