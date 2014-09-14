#include "zbus.h"
#include "ThostFtdcMdApi.h"
#include "MdSpi.h"
#include "hash.h"
#include "json.h"

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

	char* log_path;
} proxy_cfg_t;


proxy_cfg_t*  g_proxy_cfg;

proxy_cfg_t*
proxy_cfg_new(int argc, char* argv[]){
	proxy_cfg_t* self = (proxy_cfg_t*)malloc(sizeof(*self));
	assert(self);
	memset(self, 0, sizeof(proxy_cfg_t));

	self->verbose = atoi(option(argc, argv, "-v", "1"));
	self->debug = atoi(option(argc, argv, "-dbg", "0")); 
	self->zbus_timeout = atoi(option(argc, argv, "-zbus_t", "10000"));
	self->zbus_reconnect_timeout = atoi(option(argc, argv, "-zbus_r", "3000")); 
	self->worker_threads = atoi(option(argc, argv, "-c", "1"));
	self->service_name = strdup(option(argc, argv, "-s", "Trade"));
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

typedef struct _cpt_quote{
	char* zbus_client_id;
	CThostFtdcMdApi* api;
	CThostFtdcMdSpi* spi;

	int request_id; 
	char* broker_id;
	char* investor_id;
	char* password;
}ctp_t;

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


msg_t* handleFrontRegister(){
	pUserApi = CThostFtdcMdApi::CreateFtdcMdApi();			// 创建UserApi
	CThostFtdcMdSpi* pUserSpi = new CMdSpi();
	pUserApi->RegisterSpi(pUserSpi);						// 注册事件类
	pUserApi->RegisterFront(FRONT_ADDR);					// connect
	pUserApi->Init();

	json_t* res = json_object();
	json_object_addstr(res, "event", "FrontConnected");
	json_object_addstr(res, "data", "front connected");
	
	char* str = json_dump(res);
	json_destroy(res);

	msg_t* msg;
	msg = msg_new();
	msg_set_status(msg, "200");
	msg_set_body_nocopy(msg, str, strlen(str)); 
	
	return msg;
}

msg_t* handleUserLogin(){
	json_t* res = json_object();
	json_object_addstr(res, "event", "RspUserLogin");
	json_object_addstr(res, "data", "RspUserLogin");

	char* str = json_dump(res);
	json_destroy(res);

	msg_t* msg;
	msg = msg_new();
	msg_set_status(msg, "200");
	msg_set_body_nocopy(msg, str, strlen(str)); 

	return msg;
}

#define REGISTER_FRONT "RegisterFront" 
#define USER_LOGIN "UserLogin"

msg_t* my_msg_handler(msg_t* msg, void* privdata){
	msg_print(msg); 
	json_t* json = unpack_json_object(msg);
	
	json_t* cmd = json_object_item(json, "cmd");
	assert(cmd);
	if( strcmp(cmd->valuestring, REGISTER_FRONT) == 0){
		msg = handleFrontRegister(); 
	} else if ( strcmp(cmd->valuestring, USER_LOGIN) == 0) {
		msg = handleUserLogin();
	} else {
		msg = msg_new();
		msg_set_status(msg, "200");
		msg_set_body(msg, "ok") ;
	} 

	json_destroy(json); 
	return msg;
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


	}  
}

void ctp2zbus(void* args){

}


int main(int argc, char* argv[]){

	rpc_cfg_t* cfg = rpc_cfg_new();
	strcpy(cfg->mq, "CTP_QUOTE");
	strcpy(cfg->broker, "127.0.0.1:15555");
	cfg->handler = my_msg_handler;
	rpc_serve(cfg); 
	rpc_cfg_destroy(&cfg);
	return 0;
}


