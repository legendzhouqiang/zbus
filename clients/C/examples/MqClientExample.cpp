#include "MqClient.h" 


int main(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_DEBUG);

	MqClient client("localhost:15555");
	client.connect();
	 
	Message msg;
	msg.setCmd("server");
	msg.setBody("hello world");

	client.send(msg);
	string msgid = msg.getId();
	int rc;
	Message* res = client.recv(rc, msgid.c_str());
	if (res) { 
		delete res;
	} 

	system("pause");
}