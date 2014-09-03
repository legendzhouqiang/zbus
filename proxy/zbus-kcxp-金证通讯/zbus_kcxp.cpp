#include "platform.h"
#include "log.h"
#include "json.h"
#include "zbus.h"  
#include "thread.h"
#include "KCBPCli.h"


typedef struct _kcxp_cfg_t{ 
	char*	kcxp_name;
	char*	kcxp_host;
	int		kcxp_port;
	char*	sendq_name;
	char*	recvq_name; 
	int		kcxp_reconnect;
	
	int     kcxp_timeout;
	int     zbus_timeout;
	int     zbus_reconnect_timeout;

	char*	auth_user;
	char*	auth_pwd;

	int		verbose;
	int		debug;
	char*	broker; 
	char*	service_name;
	char*	service_regtoken;
	char*	service_acctoken;
	int		worker_threads;

	int		probe_interval;

	char*   log_path;

} kcxp_cfg_t;

kcxp_cfg_t*		g_kcbp_cfg; 



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


kcxp_cfg_t*
kcxp_cfg_new(int argc, char* argv[]){
	kcxp_cfg_t* self = (kcxp_cfg_t*)malloc(sizeof(kcxp_cfg_t));
	assert(self);
	memset(self, 0, sizeof(kcxp_cfg_t));
	self->kcxp_name = strdup(option(argc,argv, "-x", "zbus_kcxp")); 
	self->kcxp_host = strdup(option(argc,argv, "-h", "172.24.180.216"));
	self->kcxp_port = atoi(option(argc,argv,"-p","21000"));
	self->kcxp_reconnect = atoi(option(argc,argv,"-r","1000"));
	self->sendq_name = strdup(option(argc,argv, "-qs", "req_zb"));
	self->recvq_name = strdup(option(argc,argv, "-qr", "ans_zb"));
	self->auth_user = strdup(option(argc,argv, "-u", "KCXP00"));
	self->auth_pwd = strdup(option(argc,argv, "-P", "888888"));
	
	self->kcxp_timeout = atoi(option(argc,argv,"-kcxp_t","10000"));
	self->zbus_timeout = atoi(option(argc,argv,"-zbus_t","10000"));
	self->zbus_reconnect_timeout = atoi(option(argc,argv,"-zbus_r","10000"));
	
	self->verbose = atoi(option(argc,argv, "-v", "1"));
	self->debug = atoi(option(argc,argv, "-dbg", "0"));
	self->worker_threads = atoi(option(argc,argv, "-c", "2"));
	self->broker = strdup(option(argc,argv, "-b", "localhost:15555"));
	self->service_name = strdup(option(argc,argv, "-s", "KCXP"));
	self->service_regtoken = strdup(option(argc,argv, "-kreg", ""));
	self->service_acctoken = strdup(option(argc,argv, "-kacc", ""));
	self->probe_interval = atoi(option(argc,argv, "-t", "6000"));

	self->log_path = strdup(option(argc,argv, "-log", NULL));
	return self;
}

void
kcxp_cfg_destroy(kcxp_cfg_t** self_p){
	assert(self_p);
	kcxp_cfg_t* self = *self_p;
	if(self){ 
		if(self->kcxp_name)
			free(self->kcxp_name);
		if(self->kcxp_host)
			free(self->kcxp_host);
		if(self->sendq_name)
			free(self->sendq_name);
		if(self->recvq_name)
			free(self->recvq_name);
		if(self->auth_user)
			free(self->auth_user);
		if(self->auth_pwd)
			free(self->auth_pwd);
		if(self->service_name)
			free(self->service_name);
		if(self->broker)
			free(self->broker);
		if(self->log_path)
			free(self->log_path);
		free(self);
	}
}

void
kcxpcli_destroy(void** self_p){
	assert(self_p);
	void* self = *self_p;
	if(self){
		KCBPCLI_DisConnectForce(self);	
		KCBPCLI_Exit(self);
		*self_p = NULL;
	}
}


void* 
kcxpcli_new(kcxp_cfg_t* cfg){
	KCBPCLIHANDLE self; 
	tagKCBPConnectOption conn; 

	int rc;
	rc = KCBPCLI_Init(&self); 
	assert(rc == 0);

	memset(&conn, 0, sizeof(conn)); 
	strcpy(conn.szServerName, cfg->kcxp_name); 
	conn.nProtocal = 0; 
	strcpy(conn.szAddress, cfg->kcxp_host); 
	conn.nPort = cfg->kcxp_port; 
	strcpy(conn.szSendQName, cfg->sendq_name); 
	strcpy(conn.szReceiveQName, cfg->recvq_name); 

	KCBPCLI_SetOptions(self, KCBP_OPTION_CONNECT, &conn, sizeof(conn));

	int auth = 0; //for asynchronise mode
	KCBPCLI_SetOptions(self, KCBP_OPTION_AUTHENTICATION, &auth, sizeof(auth));

	zlog("KCXP Connecting ...\n");
	rc = KCBPCLI_ConnectServer(self, cfg->kcxp_name, cfg->auth_user, cfg->auth_pwd); 

	if(rc != 0){
		zlog("KCXP Connect failed: %d\n", rc);
		kcxpcli_destroy(&self);
	} else {
		zlog("KCXP Connected\n", rc);
	}
	return self;
}


void
kcxpcli_clear_data(void* kcbp){
	assert(kcbp);
	do {
		while( KCBPCLI_SQLFetch(kcbp) == 0 );
	}while( KCBPCLI_SQLMoreResults( kcbp ) == 0 );

	KCBPCLI_SQLCloseCursor(kcbp);
}

void* zbus2kcxp(void* args){
	rclient_t* client;
	consumer_t* zbus_consumer; 

	client = rclient_connect(g_kcbp_cfg->broker, g_kcbp_cfg->zbus_reconnect_timeout);
	zbus_consumer = consumer_new(client, g_kcbp_cfg->service_name, MODE_MQ);
	consumer_set_acc_token(zbus_consumer, g_kcbp_cfg->service_acctoken);
	consumer_set_reg_token(zbus_consumer, g_kcbp_cfg->service_regtoken);
  

	void* kcbp = kcxpcli_new(g_kcbp_cfg); 
	char error_msg[1024] = {0}; 
	tagCallCtrl bpctrl; 
	int rc; 

	while(1){  
		msg_t* res = NULL;
		int rc; 

		rc = consumer_recv(zbus_consumer, &res, g_kcbp_cfg->zbus_timeout);
		if(rc < 0) continue;
		if( !res ) continue;

		msg_print(res);

		char* broker = msg_get_head_or_param(res, HEADER_BROKER);
		char* mq_reply = msg_get_mq_reply(res);
		char* msgid = msg_get_msgid_raw(res);  

		char* bodystr = msg_copy_body(res); //to free
		printf("%s\n", bodystr);
		json_t* json = json_parse(bodystr);
		
		json_t* funcid_json = json_object_item(json, "method");
		char* funcid = funcid_json->valuestring;
		printf("method: %s\n", funcid);

		json_t* params_array_json = json_object_item(json, "params");
		json_t* params_json = json_array_item(params_array_json, 0);
		
		KCBPCLI_BeginWrite(kcbp);
		json_t* param = params_json->child;
		while(param){
			if(param->string[0] == '@'){
				param = param->next;
				continue;
			}
			//handle binary type
			printf("%s=>%s\n", param->string, param->valuestring);

			KCBPCLI_SetValue(kcbp, param->string,param->valuestring);
			param = param->next;
		}

		memset(&bpctrl, 0, sizeof(bpctrl));
		bpctrl.nExpiry = g_kcbp_cfg->kcxp_timeout/1000; //s
		bpctrl.szMsgId


		if(g_kcbp_cfg->debug)
			zlog("KCXP AsynCall BEGIN....\n");

		rc = KCBPCLI_ACallProgramAndCommit(kcbp, funcid, &bpctrl);  

		if(g_kcbp_cfg->debug)
			zlog("KCXP AsynCall END (%d)\n", rc);
		
		if(rc == 0){ 

		} else {

		}

	}
	return NULL;
}

void* kcxp2zbus(void* args){
	return NULL;
}

int main(int argc, char *argv[]){

	g_kcbp_cfg = kcxp_cfg_new(argc, argv); 

	if(g_kcbp_cfg->log_path){
		zlog_set_file(g_kcbp_cfg->log_path); 
	} else {
		zlog_set_stdout();
	}

	char* broker = strdup(g_kcbp_cfg->broker);

	int thread_count = g_kcbp_cfg->worker_threads;
	pthread_t* zbus2msmq_threads = (pthread_t*)malloc(thread_count*sizeof(pthread_t));
	pthread_t* msmq2zbus_threads = (pthread_t*)malloc(thread_count*sizeof(pthread_t));

	for(int i=0; i<thread_count; i++){
		pthread_create(&zbus2msmq_threads[i], NULL, zbus2kcxp, NULL); 
		pthread_create(&msmq2zbus_threads[i], NULL, kcxp2zbus, NULL); 
	} 

	free(broker);

	for(int i=0; i<thread_count; i++){
		pthread_join(&zbus2msmq_threads[i], NULL);
		pthread_join(&msmq2zbus_threads[i], NULL);
	}  

	kcxp_cfg_destroy(&g_kcbp_cfg);

	getchar();
	return 0;
}