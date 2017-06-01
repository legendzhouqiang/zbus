#include "zbus.h" 

int main_jsonrpc_send(int argc, char* argv[]){
	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000);
	producer_t* p = producer_new(client, "MyJsonRpc");
	msg_t* msg, *res = NULL;
	int rc,i;
	
	for(i=0;i<10;i++){ 
		json_t* req = json_object();
		json_t* params = json_array();
		json_array_add(params, json_number(1));
		json_array_add(params, json_number(2));

		json_object_addstr(req, "module", "Interface");
		json_object_addstr(req, "method", "plus");
		json_object_add(req, "params", params);
		

		msg = pack_json_request(req);
		msg_set_recver(msg, "MyRpcReply");

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