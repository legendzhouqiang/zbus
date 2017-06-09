#include "RpcInvoker.h"


int main(int argc, char* argv[]) {  
	Logger::configDefaultLogger(0, LOG_INFO);  

	Broker broker("localhost:15555"); 

	RpcInvoker rpc(&broker, "MyRpc");

	Request req;
	req.method = "plus";
	req.params.push_back(1);
	req.params.push_back(2); 

	Response res = rpc.invoke(req);

	broker.join(); 
	return 0;
}