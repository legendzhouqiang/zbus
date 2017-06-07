#include "MqClient.h" 

void onMessage(Message* msg) {
	msg->print();
	delete msg;
}

void onConnected(MessageClient* client) {
	Message msg;
	msg.setCmd("track_sub");
	client->send(msg);
}


int main(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_INFO);

	MessageClient client("localhost:15555");
	client.onMessage = onMessage;
	client.connected = onConnected;
	client.start();

	client.join();
	system("pause");
	return 0;
}