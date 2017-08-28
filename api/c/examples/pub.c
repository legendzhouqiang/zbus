#include "zbus.h" 

int main_pub(int argc, char* argv[]){
	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000);
	producer_t* p = producer_new(client, "MySub", MODE_PUBSUB);
	msg_t* msg, *res = NULL;
	int rc,i;
	

	for(i=0;i<100;i++){
		msg = msg_new();
		msg_set_topic(msg, "qhee");
		msg_set_bodyfmt(msg, "hello %lld", current_millis());

		rc = producer_send(p, msg, &res, 10000);
		if(rc>=0 && res){
			msg_print(res);
			msg_destroy(&res);
		}
	}

	getchar();
	producer_destroy(&p);
	rclient_destroy(&client);
	return 0;
}