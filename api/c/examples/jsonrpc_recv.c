#include "zbus.h"


int main_jsonrpc_recv(int argc, char* argv[]){
	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000);
	consumer_t* consumer = consumer_new(client, "MyJsonRpcReply", MODE_MQ);
	msg_t*res = NULL;
	int rc,i=0;
	while(1){
		rc = consumer_recv(consumer, &res, 10000);
		if(rc<0) continue;
		i++;
		if(rc>=0 && res){
			json_t* json = unpack_json_object(res);
			char* str = json_dump(json);
			printf("%s\n", str);
			free(str);
			json_destroy(json);
		}
	}
	getchar();
	consumer_destroy(&consumer);
	rclient_destroy(&client);
	return 0;
}