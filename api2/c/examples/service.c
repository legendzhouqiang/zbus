#include "zbus.h" 

msg_t* my_msg_handler(msg_t* msg, void* privdata){
	msg_print(msg);
	msg_destroy(&msg);
	
	msg = msg_new();
	msg_set_status(msg, "200");
	msg_set_body(msg, "service from c");
	return msg;
}


int main_service(int argc, char* argv[]){
	service_cfg_t* cfg = service_cfg_new();
	strcpy(cfg->mq, "MyService");
	strcpy(cfg->broker, "127.0.0.1:15555");
	cfg->handler = my_msg_handler;
	service_serve(cfg); 
	service_cfg_destroy(&cfg);
	return 0;
}