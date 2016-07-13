#include "../zbus.h"

int main_producer(int argc, char* argv[]){
	//create RemotingClient
	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000); 
	//create Producer by MQ and RemotingClient
	producer_t* p = producer_new(client, "MyMQ", MODE_MQ);

	msg_t* msg, *res = NULL;
	int rc;

	msg = msg_new();
	msg_set_body(msg, "hello world"); 
	rc = producer_send(p, msg, &res, 10000);
	if(rc>=0 && res){
		msg_print(res);
		msg_destroy(&res);
	} 

	getchar();
	producer_destroy(&p);
	rclient_destroy(&client);
	return 0;
}
