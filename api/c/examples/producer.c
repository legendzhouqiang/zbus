#include "zbus.h"

int main_producer(int argc, char* argv[]){
    //1)创建通讯链接
	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000);
	//2)创建生产对象，指定队列
	producer_t* p = producer_new(client, "MyMQ", MODE_MQ);
	
	msg_t* msg, *res = NULL;
	int rc;
	
	msg = msg_new();
	msg_set_body(msg, "hello world");
	//3)生产消息	
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