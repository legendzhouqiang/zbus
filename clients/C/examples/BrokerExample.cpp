#include "Broker.h"  


int main(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_INFO); 
	Logger* log = Logger::getLogger(); 


	Broker broker("localhost:15555");

	system("pause");
	return 0;
}