#include "zbus.h"
#include "ThostFtdcMdApi.h"
#include "MdSpi.h"
#include "hash.h"
#include "json.h"
#include "log.h"
#include "thread.h"

static char*
option(int argc, char* argv[], char* opt, char* default_value){
	int i,len;
	char* value = default_value;
	for(i=1; i<argc; i++){
		len = strlen(opt);
		if(len> strlen(argv[i])) len = strlen(argv[i]);
		if(strncmp(argv[i],opt,len)==0){
			value = &argv[i][len];
		}
	}
	return value;
}

typedef struct _proxy_cfg_t{ 
	int   zbus_timeout;
	int   zbus_reconnect_timeout;

	int   worker_threads;
	int   verbose;
	int	  debug;
	char* service_name; 
	char* service_regtoken;
	char* service_acctoken;
	char* broker;

	char* front_addr;

	char* log_path;
} proxy_cfg_t;

typedef struct _cpt_quote{
	char* zbus_client_id;
	CThostFtdcMdApi* api;
	CThostFtdcMdSpi* spi;

	int request_id; 
	char* broker_id;
	char* investor_id;
	char* password;
}ctp_t;


proxy_cfg_t*  g_proxy_cfg;

proxy_cfg_t*
proxy_cfg_new(int argc, char* argv[]){
	proxy_cfg_t* self = (proxy_cfg_t*)malloc(sizeof(*self));
	assert(self);
	memset(self, 0, sizeof(proxy_cfg_t));

	self->front_addr = strdup(option(argc,argv, "-ctp_addr", "tcp://asp-sim2-md1.financial-trading-platform.com:26213"));

	self->verbose = atoi(option(argc, argv, "-v", "1"));
	self->debug = atoi(option(argc, argv, "-dbg", "0")); 
	self->zbus_timeout = atoi(option(argc, argv, "-zbus_t", "10000"));
	self->zbus_reconnect_timeout = atoi(option(argc, argv, "-zbus_r", "3000")); 
	self->worker_threads = atoi(option(argc, argv, "-c", "1"));
	self->service_name = strdup(option(argc, argv, "-s", "CtpQuote"));
	self->service_regtoken = strdup(option(argc,argv, "-kreg", ""));
	self->service_acctoken = strdup(option(argc,argv, "-kacc", ""));
	self->broker = strdup(option(argc,argv, "-b", "localhost:15555"));
	self->log_path = strdup(option(argc,argv, "-log", NULL));

	return self;
}

void
proxy_cfg_destroy(proxy_cfg_t** self_p){
	assert(self_p);
	proxy_cfg_t* self = *self_p;
	if(self){
		if(self->front_addr){
			free(self->front_addr);
		}

		if(self->service_name)
			free(self->service_name);
		if(self->broker)
			free(self->broker);
		if(self->service_regtoken)
			free(self->service_regtoken);
		if(self->service_acctoken)
			free(self->service_acctoken);
		if(self->log_path)
			free(self->log_path);

		free(self);
		*self_p = NULL;
	}
} 


// UserApi对象
CThostFtdcMdApi* pUserApi;

// 配置参数
char FRONT_ADDR[] = "tcp://asp-sim2-md1.financial-trading-platform.com:26213";		// 前置地址
TThostFtdcBrokerIDType	BROKER_ID = "2030";				// 经纪公司代码
TThostFtdcInvestorIDType INVESTOR_ID = "00092";			// 投资者代码
TThostFtdcPasswordType  PASSWORD = "888888";			// 用户密码
char *ppInstrumentID[] = {"IF1409", "cu0909"};			// 行情订阅列表
int iInstrumentID = 2;									// 行情订阅数量

// 请求编号
int iRequestID = 0;


hash_ctrl_t hash_ctrl_sock2ctp = {
	hash_func_string,			/* hash function */
	hash_dup_string,			/* key dup */
	NULL,				        /* val dup */
	hash_cmp_string,			/* key compare */
	hash_destroy_string,		/* key destructor */
	NULL,			            /* val destructor */	
};

hash_t* g_sock2ctp;

hash_ctrl_t hash_ctrl_cmd2handler = {
	hash_func_string,			/* hash function */
	hash_dup_string,			/* key dup */
	NULL,				        /* val dup */
	hash_cmp_string,			/* key compare */
	hash_destroy_string,		/* key destructor */
	NULL,			            /* val destructor */	
};

hash_t* g_cmd2handler;


ctp_t* ctp_new(){
	ctp_t* self = (ctp_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	return self;
}

void ctp_destroy(ctp_t** self_p){
	if(!self_p) return;
	ctp_t* self = *self_p;
	if(self->zbus_client_id){
		free(self->zbus_client_id);
	}
	if(self->broker_id){
		free(self->broker_id);
	}
	if(self->investor_id){
		free(self->investor_id);
	}
	if(self->password){
		free(self->password);
	}
	if(self->spi){
		delete self->spi;
	}
	if(self->api){
		self->api->Release(); 
	}
	free(self);
	*self_p = NULL;
}

int ctp_register_front(ctp_t* self, char* front_addr){
	if(self->api){
		return -1; //已经注册
	}
	self->api = CThostFtdcMdApi::CreateFtdcMdApi();
	self->spi = new CMdSpi();
	self->api->RegisterSpi(self->spi);
	self->api->RegisterFront(front_addr);
	self->api->Init();
	
	return 0;
}


typedef struct req_ctx{ 
	consumer_t* consumer;
	void* ctp; //ctp or sockid
	json_t* params;
	msg_t* msg;
}req_ctx_t;

typedef void (cmd_handler)(req_ctx_t* ctx);

static int s_reply(consumer_t* consumer, msg_t* req,  char* code, char* content){
	msg_t* msg = msg_new(); 
	msg_set_status(msg, code); 
	msg_set_body(msg, content); 
	msg_set_mq_reply(msg, msg_get_mq_reply(req)); 

	return consumer_reply(consumer, msg);
}



void handle_front_register(req_ctx_t* ctx){ 
	char* sockid = (char*)ctx->ctp;
	ctp_t* ctp = (ctp_t*) hash_get(g_sock2ctp, sockid); //线程安全???
	if(ctp != NULL){  
		s_reply(ctx->consumer, ctx->msg, "200", "ok");
		return;
	}

	ctp = ctp_new();
	hash_put(g_sock2ctp, sockid, ctp);

}

void* zbus2ctp(void* args){
	rclient_t* client;
	consumer_t* zbus_consumer; 

	client = rclient_connect(g_proxy_cfg->broker, g_proxy_cfg->zbus_reconnect_timeout);
	zbus_consumer = consumer_new(client, g_proxy_cfg->service_name, MODE_MQ);
	consumer_set_acc_token(zbus_consumer, g_proxy_cfg->service_acctoken);
	consumer_set_reg_token(zbus_consumer, g_proxy_cfg->service_regtoken);


	while(1){
		msg_t* msg = NULL;
		int rc = consumer_recv(zbus_consumer, &msg, g_proxy_cfg->zbus_timeout);
		if(rc < 0) continue;
		if( !msg ) continue;
		
		msg_print(msg);	

		char* sockid = msg_get_head_or_param(msg, "sockid-src"); //
		if(sockid == NULL){
			s_reply(zbus_consumer, msg, "400", "zbus broker: missing sockid");
			goto destroy;
		}

		ctp_t* ctp = (ctp_t*) hash_get(g_sock2ctp, sockid);

		json_t* json = unpack_json_object(msg);
		json_t* cmd = json_object_item(json, "cmd");
		json_t* params = json_object_item(json, "params");
		
		req_ctx_t ctx = {
			zbus_consumer,
			NULL,
			params,
			msg
		};
		ctx.ctp = ctp?  (void*)ctp : (void*)sockid;
		
		cmd_handler* handler = (cmd_handler*)hash_get(g_cmd2handler, cmd->valuestring);
		
		if(handler == NULL){
			char errmsg[256];
			sprintf(errmsg, "command(%s) not support", cmd->valuestring);
			s_reply(zbus_consumer, msg, "404", errmsg);
			goto destroy;
		}
		
		handler(&ctx);

destroy:
		json_destroy(json);
	} 
	return NULL;
}

void* ctp2zbus(void* args){

	return NULL;
}

#define REGISTER_FRONT "RegisterFront" 
#define USER_LOGIN "UserLogin"
void s_init_cmd_handlers(){
	g_cmd2handler = hash_new(&hash_ctrl_cmd2handler, NULL);
	hash_put(g_cmd2handler, REGISTER_FRONT, handle_front_register);
}


int main(int argc, char* argv[]){   
	g_proxy_cfg = proxy_cfg_new(argc, argv); 
	if(g_proxy_cfg->log_path){
		zlog_set_file(g_proxy_cfg->log_path); 
	} else {
		zlog_set_stdout();
	}

	g_sock2ctp = hash_new(&hash_ctrl_sock2ctp, NULL);
	s_init_cmd_handlers();


	int thread_count = g_proxy_cfg->worker_threads;
	pthread_t* zbus2ctp_threads = (pthread_t*)malloc(thread_count*sizeof(pthread_t));
	pthread_t* ctp2zbus_threads = (pthread_t*)malloc(thread_count*sizeof(pthread_t));

	for(int i=0; i<thread_count; i++){
		pthread_create(&zbus2ctp_threads[i], NULL, zbus2ctp, NULL); 
		pthread_create(&ctp2zbus_threads[i], NULL, ctp2zbus, NULL); 
	}  

	for(int i=0; i<thread_count; i++){
		pthread_join(&zbus2ctp_threads[i], NULL);
		pthread_join(&ctp2zbus_threads[i], NULL);
	}  

	proxy_cfg_destroy(&g_proxy_cfg); 
	hash_destroy(&g_sock2ctp);
	hash_destroy(&g_cmd2handler);
	return 0;
}

