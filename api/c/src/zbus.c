#include "zbus.h"
#include "net.h"
#include "thread.h"
#include "json.h"
//////////////////////////////Producer//////////////////////////

struct producer{
	rclient_t* client;
	char* mq;
	char* token;
	int   mode;
};

producer_t* producer_new(rclient_t* client, char* mq, int mode){
	producer_t* self;
	assert(client);
	assert(mq);
	self = malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->client = client;
	self->mq = strdup(mq);
	self->token = strdup("");
	self->mode = mode;
	return self;
}

void producer_destroy(producer_t** self_p){
	producer_t* self;
	assert(self_p);
	self = *self_p;
	if(!self) return;
	if(self->mq) 
		free(self->mq);
	if(self->token)
		free(self->token); 
	free(self);
	*self_p = NULL;
}

void producer_set_token(producer_t* self, char* token){
	if(self->token){
		free(self->token);
	}
	self->token = strdup(token);
}

int producer_send(producer_t* self, msg_t* msg, msg_t** result_p, int timeout){
	assert(self);
	assert(msg);
	msg_set_command(msg, PRODUCE);
	msg_set_mq(msg, self->mq);
	msg_set_token(msg, self->token);

	return rclient_invoke(self->client, msg, result_p, timeout);
}


struct consumer{
	rclient_t* client;
	char* mq;
	char* acc_token;
	char* reg_token;
	int   mode;
	char* topic;
	int   auto_register;
	int   reconnect_millis;
};

consumer_t* consumer_new(rclient_t* client, char* mq, int msg_mode){
	consumer_t* self;
	assert(client);
	assert(mq);
	self = malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->client = client;
	self->mq = strdup(mq);
	self->acc_token = strdup("");
	self->reg_token = strdup("");
	self->mode = msg_mode;
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
	if(self->acc_token)
		free(self->acc_token);
	if(self->reg_token)
		free(self->reg_token); 
	free(self);
	*self_p = NULL;
}

void consumer_set_acc_token(consumer_t* self, char* value){
	assert(self);
	if(self->acc_token){
		free(self->acc_token);
	}
	self->acc_token = strdup(value);
}
void consumer_set_reg_token(consumer_t* self, char* value){
	assert(self);
	if(self->reg_token){
		free(self->reg_token);
	}
	self->reg_token = strdup(value);
}
void consumer_set_topic(consumer_t* self, char* value){
	assert(self);
	if(self->topic){
		free(self->topic);
	}
	self->topic = strdup(value);
}

static int consumer_register(consumer_t* self, int timeout){
	int rc;
	char mode[64];
	msg_t* res = NULL;
	msg_t* msg = msg_new(); 
	msg_set_command(msg, ADMIN);
	msg_set_token(msg, self->reg_token);
	msg_set_head(msg, "cmd", CREATE_MQ);
	msg_set_head(msg, "mq_name", self->mq);
	msg_set_head(msg, "access_token", self->acc_token);
	sprintf(mode, "%d", self->mode);
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
	msg_set_command(msg, CONSUME);
	msg_set_mq(msg, self->mq);
	msg_set_token(msg, self->acc_token);
	if(self->mode & MODE_PUBSUB){
		if(self->topic){
			msg_set_topic(msg, self->topic);
		}
	}

	rc = rclient_invoke(self->client, msg, result_p, timeout);
	
	if(rc == ERR_NET_RECV_FAILED){ //超时
		return rc;
	}
	
	if(rc < 0){ //重新连接
		consumer_fail_over(self);
		return consumer_recv(self, result_p, timeout);
	}

	msg = *result_p;
	if(msg_is_status404(msg) && self->auto_register){
		//register
		consumer_register(self, timeout);
		return consumer_recv(self, result_p, timeout);
	} 
	return rc; 
}

int consumer_reply(consumer_t* self, msg_t* msg){
	assert(self);
	assert(msg);
	msg_set_head(msg, HEADER_REPLY_CODE, msg_get_status(msg));
	msg_set_command(msg, PRODUCE);
	msg_set_mq(msg, msg_get_mq_reply(msg));
	msg_set_ack(msg, false);
	return rclient_send(self->client, msg);
}


////////////////////////////////////////RPC//////////////////////////////////////
struct rpc{
	rclient_t* client;
	char* mq;
	char* token;
	char* encoding;
};


rpc_t* rpc_new(rclient_t* client, char* mq){
	rpc_t* self;
	assert(client);
	assert(mq);
	self = malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->client = client;
	self->mq = strdup(mq);
	self->token = strdup("");
	self->encoding = strdup("utf8");

	return self;
}

void rpc_destroy(rpc_t** self_p){
	rpc_t* self;
	assert(self_p);
	self = *self_p;
	if(!self) return;
	if(self->mq) 
		free(self->mq);
	if(self->token)
		free(self->token);
	if(self->encoding)
		free(self->encoding);
	free(self);
	*self_p = NULL;
}

void rpc_set_token(rpc_t* self, char* value){
	if(self->token){
		free(self->token);
	}
	self->token = strdup(value);
}
void rpc_set_encoding(rpc_t* self, char* value){
	if(self->encoding){
		free(self->encoding);
	}
	self->encoding = strdup(value);
}

int rpc_invoke(rpc_t* self, msg_t* request, msg_t** result_p, int timeout){
	assert(self);
	assert(request);
	msg_set_command(request, REQUEST);
	msg_set_mq(request, self->mq);
	msg_set_token(request, self->token);
	msg_set_encoding(request, self->encoding);

	return rclient_invoke(self->client, request, result_p, timeout);
}
/////////////////////////////
struct jsonrpc{
	rpc_t* rpc;
	char*  module;
};
jsonrpc_t* jsonrpc_new(rclient_t* client, char* mq){
	jsonrpc_t* self = malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->rpc = rpc_new(client, mq);
	self->module = strdup("");
	return self;
}

void jsonrpc_destroy(jsonrpc_t** self_p){
	jsonrpc_t* self;
	assert(self_p);
	self = *self_p;
	if(!self) return;
	if(self->rpc){
		rpc_destroy(&self->rpc);
	}
	if(self->module){
		free(self->module);
	}
	free(self);
	*self_p = NULL;
}

jsonrpc_t* jsonrpc_set_token(jsonrpc_t* self, char* value){
	rpc_set_token(self->rpc, value);
	return self;
}

jsonrpc_t* jsonrpc_set_encoding(jsonrpc_t* self, char* value){
	rpc_set_encoding(self->rpc, value);
	return self;
}

jsonrpc_t* jsonrpc_set_module(jsonrpc_t* self, char* value){
	if(self->module){
		free(self->module);
	}
	self->module = strdup(value);
	return self;
}


int jsonrpc_invoke(jsonrpc_t* self, char* method, json_t* params, json_t** result_p, int timeout){
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

	return jsonrpc_call(self, request, result_p, timeout);
}

//request: module: string
//         method: string
//         params: json_array
//result_p:
//         result: json
//         error: error string
//         stack_trace: error string
int jsonrpc_call(jsonrpc_t* self, json_t* request, json_t** result_p, int timeout){
	int rc = 0;
	char* req_jsonstr;
	msg_t* msg;
	msg_t* res = NULL;

	req_jsonstr = json_dump(request);
	json_destroy(request);

	msg = msg_new();
	msg_set_head(msg, "content-type", "application/json");
	msg_set_body_nocopy(msg, req_jsonstr, strlen(req_jsonstr));

	rc = rpc_invoke(self->rpc, msg, &res, timeout);
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

rpc_cfg_t* rpc_cfg_new(){
	rpc_cfg_t* self = malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	strcpy(self->broker, "127.0.0.1:15555");
	strcpy(self->acc_token, "");
	strcpy(self->reg_token ,"");
	self->thread_count = 1;
	self->consume_timeout = 10000;
	self->reconnect_interval = 3000;
	return self;
}
void rpc_cfg_destroy(rpc_cfg_t** self_p){
	rpc_cfg_t* self = *self_p;
	if(!self) return;
	free(self);
	*self_p = NULL;
}
 

void* do_rpc_work(void* args){
	rpc_cfg_t* cfg = (rpc_cfg_t*)args;
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
	
	consumer = consumer_new(client, cfg->mq, MODE_MQ);
	consumer_set_reg_token(consumer, cfg->reg_token);
	consumer_set_acc_token(consumer, cfg->acc_token);
	
	
	while(1){
		msg_t* msg = NULL, *res;
		char* recver, *msgid;
		int rc = consumer_recv(consumer, &msg, timeout);
		if (rc<0 || msg == NULL){
			continue;
		}
		recver = strdup(msg_get_mq_reply(msg));
		msgid = strdup(msg_get_msgid_raw(msg));
		res = handler(msg, consumer);
		if(res){
			msg_set_msgid(res, msgid);
			msg_set_mq_reply(res, recver);
			consumer_reply(consumer, res);
		} 
		free(recver);
		free(msgid);
	}
	
	rclient_destroy(&client);
	consumer_destroy(&consumer);
}

void* rpc_serve(void* args){
	int i;
	rpc_cfg_t* cfg = (rpc_cfg_t*)args;
	pthread_t* threads;
	assert(cfg);
	assert(cfg->handler);
	assert(cfg->mq);
	assert(cfg->thread_count>0);
	threads = malloc(sizeof(pthread_t)*cfg->thread_count);
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
	msg_destroy(&msg);
	return result;
}