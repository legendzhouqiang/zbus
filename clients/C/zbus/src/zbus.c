#include "zbus.h"
#include "net.h"
#include "thread.h"
#include "json.h"
//////////////////////////////Producer//////////////////////////

struct producer{
	rclient_t* client;
	char* mq;  
};

producer_t* producer_new(rclient_t* client, char* mq){
	producer_t* self;
	assert(client);
	assert(mq);
	self = (producer_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->client = client;
	self->mq = strdup(mq);  
	return self;
}

void producer_destroy(producer_t** self_p){
	producer_t* self;
	assert(self_p);
	self = *self_p;
	if(!self) return;
	if(self->mq) 
		free(self->mq); 
	free(self);
	*self_p = NULL;
}
 

int producer_send(producer_t* self, msg_t* msg, msg_t** result_p, int timeout){
	assert(self);
	assert(msg);
	msg_set_cmd(msg, PRODUCE);
	msg_set_topic(msg, self->mq);  

	return rclient_invoke(self->client, msg, result_p, timeout);
}


struct consumer{
	rclient_t* client;
	char* mq;   
	int   auto_register;
	int   reconnect_millis;
};

consumer_t* consumer_new(rclient_t* client, char* mq){
	consumer_t* self;
	assert(client);
	assert(mq);
	self = (consumer_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->client = client;
	self->mq = strdup(mq);  
	self->auto_register = 1;
	self->reconnect_millis = 3000; //3s
	return self;
}

void consumer_destroy(consumer_t** self_p){
	consumer_t* self;
	assert(self_p);
	self = *self_p;
	if(!self) return;
	if(self->mq) 
		free(self->mq); 
	free(self);
	*self_p = NULL;
}
  

static int consumer_createmq(consumer_t* self, int timeout){
	int rc;
	char mode[64];
	msg_t* res = NULL;

	msg_t* msg = msg_new();   
	msg_set_cmd(msg, CREATE_MQ);  
	msg_set_head(msg, "mq_name", self->mq); 
	msg_set_head(msg, "mq_mode", mode);

	rc = rclient_invoke(self->client, msg, &res, timeout); 
	msg_destroy(&res);
	return rc;
}


static void consumer_fail_over(consumer_t* self){
	rclient_reconnect(self->client, self->reconnect_millis);
}


int consumer_recv(consumer_t* self, msg_t** result_p, int timeout){
	int rc;
	msg_t* msg;
	assert(self); 
	msg = msg_new();
	msg_set_cmd(msg, CONSUME);
	msg_set_topic(msg, self->mq);  

	rc = rclient_invoke(self->client, msg, result_p, timeout);
	
	if(rc == ERR_NET_RECV_FAILED){  
		return rc;
	}
	
	if(rc < 0){ 
		consumer_fail_over(self);
		return consumer_recv(self, result_p, timeout);
	}

	msg = *result_p;
	if(msg_is_status404(msg)){
		//register
		consumer_createmq(self, timeout);
		return consumer_recv(self, result_p, timeout);
	}  
	msg_set_id(msg, msg_get_rawid(msg));
	msg_remove_head(msg, HEADER_ORIGIN_ID);
	return rc; 
}

int consumer_route(consumer_t* self, msg_t* msg){
	assert(self);
	assert(msg); 
	msg_set_cmd(msg, ROUTE); 
	msg_set_ack(msg, false);
	char* status = msg_get_status(msg);
	if(status){
		msg_set_reply_code(msg, status);
		msg_set_status(msg, NULL);
	}
	return rclient_send(self->client, msg);
}


////////////////////////////////////////RPC//////////////////////////////////////
struct caller{
	rclient_t* client;
	char* mq; 
	char* encoding;
};


caller_t* caller_new(rclient_t* client, char* mq){
	caller_t* self;
	assert(client);
	assert(mq);
	self = (caller_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->client = client;
	self->mq = strdup(mq); 
	self->encoding = strdup("utf8");

	return self;
}

void caller_destroy(caller_t** self_p){
	caller_t* self;
	assert(self_p);
	self = *self_p;
	if(!self) return;
	if(self->mq) 
		free(self->mq); 
	if(self->encoding)
		free(self->encoding);
	free(self);
	*self_p = NULL;
}
 
void caller_set_encoding(caller_t* self, char* value){
	if(self->encoding){
		free(self->encoding);
	}
	self->encoding = strdup(value);
}

int caller_invoke(caller_t* self, msg_t* request, msg_t** result_p, int timeout){
	assert(self);
	assert(request);
	msg_set_cmd(request, PRODUCE);
	msg_set_topic(request, self->mq); 
	msg_set_ack(request, 0);
	msg_set_encoding(request, self->encoding);

	return rclient_invoke(self->client, request, result_p, timeout);
}
/////////////////////////////
struct _rpc{
	caller_t* rpc;
	char*  module;
};
rpc_t* rpc_new(rclient_t* client, char* mq){
	rpc_t* self = (rpc_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->rpc = caller_new(client, mq);
	self->module = strdup("");
	return self;
}

void rpc_destroy(rpc_t** self_p){
	rpc_t* self;
	assert(self_p);
	self = *self_p;
	if(!self) return;
	if(self->rpc){
		caller_destroy(&self->rpc);
	}
	if(self->module){
		free(self->module);
	}
	free(self);
	*self_p = NULL;
}


rpc_t* jsonrpc_set_encoding(rpc_t* self, char* value){
	caller_set_encoding(self->rpc, value);
	return self;
}

rpc_t* jsonrpc_set_module(rpc_t* self, char* value){
	if(self->module){
		free(self->module);
	}
	self->module = strdup(value);
	return self;
}


int rpc_invoke(rpc_t* self, char* method, json_t* params, json_t** result_p, int timeout){
	json_t* request; 
	if(params->type != JSON_ARRAY){ //
		json_t* temp = params;
		params = json_array();
		json_array_add(params, temp);
	}
	request = json_object();
	json_object_addstr(request, "module", self->module);
	json_object_addstr(request, "method", method);
	json_object_add(request, "params", params); 

	return rpc_call(self, request, result_p, timeout);
}

//request: module: string
//         method: string
//         params: json_array
//result_p:
//         result: json
//         error: error string
//         stack_trace: error string
int rpc_call(rpc_t* self, json_t* request, json_t** result_p, int timeout){
	int rc = 0;
	char* req_jsonstr;
	msg_t* msg;
	msg_t* res = NULL;

	req_jsonstr = json_dump(request);
	json_destroy(request);

	msg = msg_new();
	msg_set_head(msg, "content-type", "application/json");
	msg_set_body_nocopy(msg, req_jsonstr, strlen(req_jsonstr));

	rc = caller_invoke(self->rpc, msg, &res, timeout);
	if(rc<0 || res == NULL){
		*result_p = NULL;
		return rc;
	} else {
		char* body = msg_copy_body(res);
		json_t* result = json_parse(body);
		if(result == NULL){
			result = json_object();
			json_object_addstr(result, "error", body);
			json_object_addstr(result, "stack_trace", body);
		}
		*result_p = result; 
		free(body);
		msg_destroy(&res);
	} 
	return rc;
}



//////////////////////////////////////////////////////

service_cfg_t* service_cfg_new(){
	service_cfg_t* self = (service_cfg_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	strcpy(self->broker, "127.0.0.1:15555"); 
	self->thread_count = 1;
	self->consume_timeout = 10000;
	self->reconnect_interval = 3000;
	return self;
}
void service_cfg_destroy(service_cfg_t** self_p){
	service_cfg_t* self = *self_p;
	if(!self) return;
	free(self);
	*self_p = NULL;
}
 

void* do_rpc_work(void* args){
	service_cfg_t* cfg = (service_cfg_t*)args;
	rclient_t* client;
	consumer_t* consumer;
	service_handler* handler = cfg->handler; 
	int timeout = cfg->consume_timeout;
	int reconnect_interval = cfg->reconnect_interval;
	if(timeout <= 0){
		timeout = 10000; //10s
	}
	if(reconnect_interval<=0){
		reconnect_interval = 3000;//3s
	}
	
	client = rclient_new(cfg->broker);
	rclient_reconnect(client, cfg->reconnect_interval);
	
	consumer = consumer_new(client, cfg->mq); 
	
	
	while(1){
		msg_t* msg = NULL, *res;
		char* sender, *msgid;
		int rc = consumer_recv(consumer, &msg, timeout);
		if (rc<0 || msg == NULL){
			continue;
		}
		sender = strdup(msg_get_sender(msg));
		msgid = strdup(msg_get_id(msg));
		res = handler(msg, consumer);
		if(res){
			msg_set_id(res, msgid);
			msg_set_recver(res, sender);
			if(msg_get_status(res) == NULL){
				msg_set_status(res, "200");
			} 
			consumer_route(consumer, res);
		} 
		free(sender);
		free(msgid);
	}
	
	rclient_destroy(&client);
	consumer_destroy(&consumer);
}

void* service_serve(void* args){
	int i;
	service_cfg_t* cfg = (service_cfg_t*)args;
	pthread_t* threads;
	assert(cfg);
	assert(cfg->handler);
	assert(cfg->mq);
	assert(cfg->thread_count>0);
	threads = (u_int*)malloc(sizeof(pthread_t)*cfg->thread_count);
	for(i=0; i<cfg->thread_count; i++){
		pthread_create(&threads[i], NULL, do_rpc_work, cfg);
	}
	for(i=0; i<cfg->thread_count; i++){
		pthread_join(&threads[i], NULL);
	}
	free(threads);
	return NULL;
}
 
msg_t* pack_json_request(json_t* request){
	msg_t* msg = msg_new();
	char* json_str = json_dump(request);
	json_destroy(request);
	msg = msg_new();
	msg_set_head(msg, "content-type", "application/json");
	msg_set_body_nocopy(msg, json_str, strlen(json_str));

	return msg;
}

json_t* unpack_json_object(msg_t* msg){
	char* body = msg_copy_body(msg);
	json_t* result = json_parse(body);
	if(result == NULL){
		result = json_object();
		json_object_addstr(result, "error", body);
		json_object_addstr(result, "stack_trace", body);
	} 
	free(body);
	return result;
}
