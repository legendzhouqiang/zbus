#include "../zbus.h" 

int main_rpc(int argc, char* argv[]){
	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000);
	rpc_t* p = rpc_new(client, "MyRpc");
	json_t *res = NULL;
	int rc,i;

	for(i=0;i<1;i++){
		json_t* req = json_object();
		json_t* params = json_array();
		json_array_add(params, json_number(1));
		json_array_add(params, json_number(2));

		json_object_addstr(req, "module", "Interface");
		json_object_addstr(req, "method", "plus");
		json_object_add(req, "params", params);
		
		rc = rpc_call(p, req, &res, 10000);
		if(rc>=0 && res){
			char* str = json_dump(res);
			printf("%s\n", str);
			free(str);
			json_destroy(res);
		}
	}

	getchar();
	rpc_destroy(&p);
	rclient_destroy(&client);
	return 0;
}