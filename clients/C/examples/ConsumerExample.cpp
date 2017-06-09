#include "Consumer.h"  


int main(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_INFO);  

	Broker broker("localhost:15555");

	Consumer c(&broker, "MyTopic"); 
	c.messageHander = [](Message& msg, MqClient* client) {
		msg.print();
	};  
	c.start();  


	broker.join(); 
	return 0;
}