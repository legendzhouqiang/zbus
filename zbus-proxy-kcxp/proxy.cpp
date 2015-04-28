#include "platform.h"
#include "log.h"
#include "json.h"
#include "zbus.h"  
#include "hash.h"
#include "thread.h"
#include "KCBPCli.h"


typedef struct _proxy_cfg_t{ 
	char*	target_name;
	char*	target_host;
	int		target_port; 
	int		target_reconnect_timeout;
	int     target_timeout;
	

	char*	sendq_name;
	char*	recvq_name; 
	char*	auth_user;
	char*	auth_pwd;

	
	char*	zbus_broker; 
	int     zbus_timeout;
	int     zbus_reconnect_timeout;

	char*	service_name;
	char*	service_regtoken;
	char*	service_acctoken;
	int		worker_threads;

	int		probe_interval;

	int		verbose;
	int		debug;
	char*   log_path;

} proxy_cfg_t;

proxy_cfg_t*	g_proxy_cfg; 


hash_t*			g_msgid_target2zbus;
hash_ctrl_t		g_hctrl_msgid_target2zbus = {
	hash_func_string,			/* hash function */
	hash_dup_string,			/* key dup */
	hash_dup_string,		    /* val dup */
	hash_cmp_string,			/* key compare */
	hash_destroy_string,		/* key destructor */
	hash_destroy_string,	    /* val destructor */
};
pthread_mutex_t*	g_mutex_target2zbus;


//KCBP消息ID（动态产生） ==> zbus路由（mq_reply|msg_id）
static void 
zbus_route_record(char* mq_reply, char* msgid, char* kcxpMsgId){
	char zbus_route_info[256];
	sprintf(zbus_route_info, "%s|%s", mq_reply, msgid); 
	pthread_mutex_lock(g_mutex_target2zbus);
	hash_put(g_msgid_target2zbus, kcxpMsgId, zbus_route_info);
	pthread_mutex_unlock(g_mutex_target2zbus); 
}

static int
zbus_route_parse(char* kcxpMsgId, char* mq_reply, char* msgid){
	char* zbus_route_info = NULL;
	pthread_mutex_lock(g_mutex_target2zbus);
	zbus_route_info = (char*)hash_get(g_msgid_target2zbus, kcxpMsgId);
	if(zbus_route_info){
		zbus_route_info = strdup(zbus_route_info);
		hash_rem(g_msgid_target2zbus, kcxpMsgId);
	}
	pthread_mutex_unlock(g_mutex_target2zbus); 

	if(!zbus_route_info) return -1;
	
	int res = 0;
	char* part2 = strchr(zbus_route_info, '|');
	if(part2){
		strncpy(mq_reply, zbus_route_info, part2-zbus_route_info); 
		strcpy(msgid, part2+1);
	} else {
		res = -1;
	}
	free(zbus_route_info);

	return res;
}



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

//start zbus-kcxp -b172.24.180.27:15555 -h172.24.181.105 -v1 -c2 -r5000 -sKCXP_28 -qsreqacct_bus -qransacct_bus -xvm216-181 -loglogs
proxy_cfg_t*
proxy_cfg_new(int argc, char* argv[]){
	proxy_cfg_t* self = (proxy_cfg_t*)malloc(sizeof(proxy_cfg_t));
	assert(self);
	memset(self, 0, sizeof(proxy_cfg_t));
	self->target_name = strdup(option(argc,argv, "-x", "vm216-181")); 
	self->target_host = strdup(option(argc,argv, "-h", "172.24.181.105"));
	self->target_port = atoi(option(argc,argv,"-p","21000"));
	self->target_reconnect_timeout = atoi(option(argc,argv,"-r","1000"));
	self->target_timeout = atoi(option(argc,argv,"-kcxp_t","10000"));
	self->sendq_name = strdup(option(argc,argv, "-qs", "reqacct_bus"));
	self->recvq_name = strdup(option(argc,argv, "-qr", "ansacct_bus"));
	self->auth_user = strdup(option(argc,argv, "-u", "KCXP00"));
	self->auth_pwd = strdup(option(argc,argv, "-P", "888888"));
	
	self->zbus_broker = strdup(option(argc,argv, "-b", "localhost:15555"));
	self->service_name = strdup(option(argc,argv, "-s", "KCXP"));
	self->service_regtoken = strdup(option(argc,argv, "-kreg", ""));
	self->service_acctoken = strdup(option(argc,argv, "-kacc", ""));
	self->zbus_timeout = atoi(option(argc,argv,"-zbus_t","10000"));
	self->zbus_reconnect_timeout = atoi(option(argc,argv,"-zbus_r","10000"));
	self->worker_threads = atoi(option(argc,argv, "-c", "2"));
	self->probe_interval = atoi(option(argc,argv, "-t", "6000")); 

	
	self->verbose = atoi(option(argc,argv, "-v", "1"));
	self->debug = atoi(option(argc,argv, "-dbg", "0"));
	self->log_path = strdup(option(argc,argv, "-log", NULL));
	return self;
}

void
proxy_cfg_destroy(proxy_cfg_t** self_p){
	assert(self_p);
	proxy_cfg_t* self = *self_p;
	if(self){ 
		if(self->target_name)
			free(self->target_name);
		if(self->target_host)
			free(self->target_host);
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
		if(self->zbus_broker)
			free(self->zbus_broker);
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
kcxpcli_new(proxy_cfg_t* cfg){
	KCBPCLIHANDLE self; 
	tagKCBPConnectOption conn; 
	
	int rc;
	rc = KCBPCLI_Init(&self); 
	assert(rc == 0);

	memset(&conn, 0, sizeof(conn)); 
	strcpy(conn.szServerName, cfg->target_name); 
	conn.nProtocal = 0; 
	strcpy(conn.szAddress, cfg->target_host); 
	conn.nPort = cfg->target_port; 
	strcpy(conn.szSendQName, cfg->sendq_name); 
	strcpy(conn.szReceiveQName, cfg->recvq_name); 

	KCBPCLI_SetOptions(self, KCBP_OPTION_CONNECT, &conn, sizeof(conn));

	int auth = 0; //for asynchronise mode
	KCBPCLI_SetOptions(self, KCBP_OPTION_AUTHENTICATION, &auth, sizeof(auth));

	zlog("KCXP Connecting ...");
	rc = KCBPCLI_ConnectServer(self, cfg->target_name, cfg->auth_user, cfg->auth_pwd); 

	if(rc != 0){
		zlog("KCXP Connect failed: %d", rc);
		kcxpcli_destroy(&self);
	} else {
		zlog("KCXP Connected", rc);
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


void* zbus2target(void* args){
	rclient_t* client;
	consumer_t* zbus_consumer; 

	client = rclient_connect(g_proxy_cfg->zbus_broker, g_proxy_cfg->zbus_reconnect_timeout);
	zbus_consumer = consumer_new(client, g_proxy_cfg->service_name, MODE_MQ);
	consumer_set_acc_token(zbus_consumer, g_proxy_cfg->service_acctoken);
	consumer_set_reg_token(zbus_consumer, g_proxy_cfg->service_regtoken);
  

	void* kcbp = kcxpcli_new(g_proxy_cfg); 
	char error_msg[1024] = {0}; 
	tagCallCtrl bpctrl; 
	int rc; 

	while(1){  
		while(!kcbp){ //assume target ok, if not reconnect
			zlog("KCXP(%s:%d) down, reconnecting...", g_proxy_cfg->target_host,g_proxy_cfg->target_port);
			Sleep(g_proxy_cfg->target_reconnect_timeout);
			kcbp = kcxpcli_new(g_proxy_cfg);
		}

		msg_t* res = NULL;
		int rc; 

		rc = consumer_recv(zbus_consumer, &res, g_proxy_cfg->zbus_timeout);
		if(rc < 0) continue;
		if( !res ) continue;

		char* broker = msg_get_head_or_param(res, HEADER_BROKER);
		char* mq_reply = msg_get_mq_reply(res);
		char* msgid = msg_get_msgid_raw(res);  
		


		char* bodystr = msg_copy_body(res);
		if(g_proxy_cfg->verbose){
			const int max_len = 4096;
			if(strlen(bodystr)>max_len){
				char temp[max_len+1] = {0};
				strncpy(temp, bodystr, max_len);
				zlog("ZBUS(MsgID=%s)=>KCXP 请求:\n%s", msgid, temp);
			}
			zlog("ZBUS(MsgID=%s)=>KCXP 请求:\n%s", msgid, bodystr);
		}

		json_t* json = json_parse(bodystr); 
		free(bodystr);
		
		json_t* funcid_json = json_object_item(json, "method");
		if(funcid_json == NULL){
			goto destroy;
		}
		char* funcid = funcid_json->valuestring; 

		json_t* params_array_json = json_object_item(json, "params");
		if(params_array_json == NULL){
			goto destroy;
		}

		json_t* params_json = json_array_item(params_array_json, 0);
		json_t* param = NULL;
		if(params_json){
			param = params_json->child;
		}

		KCBPCLI_BeginWrite(kcbp);
		while(param){
			char* key = param->string;
			if(strlen(key) == 0) {
				param = param->next;
				continue;
			} 
			if(param->valuestring == NULL){
				param = param->next;
				continue;
			}

			if(key[0] == '@'){ //特殊的json字符过滤
				param = param->next;
				continue;
			}

			//KCBP limit!!!, special for binary key-value
			if(strchr(key,'^') == key && strrchr(key,'^') == (key+strlen(key)-1)){
				char bin_key[1024]; 
				strncpy(bin_key, key+1, strlen(key)-2);
				bin_key[strlen(key)-2] = '\0';
				size_t len;
				unsigned char* val = json_base64bin(param, &len);
				KCBPCLI_SetVal(kcbp, bin_key, val, len); 
				free(val);
			} else {
				KCBPCLI_SetValue(kcbp, key, param->valuestring); 
			}   
			param = param->next;
		}
		

		memset(&bpctrl, 0, sizeof(bpctrl));
		int expiry = g_proxy_cfg->target_timeout/1000; //s
		if(expiry == 0) expiry = 10;
		bpctrl.nExpiry = expiry;  
		

		if(g_proxy_cfg->debug)
			zlog("KCXP AsynCall BEGIN....");

		rc = KCBPCLI_ACallProgramAndCommit(kcbp, funcid, &bpctrl);  
		if(rc == 0){ 
			zbus_route_record(mq_reply, msgid, bpctrl.szMsgId);
		}
		if(g_proxy_cfg->debug)
			zlog("KCXP AsynCall END (%d)", rc);
		
		if(rc != 0){ 
			KCBPCLI_GetErrorMsg(kcbp, error_msg);
			zlog("WARN: KCXP failed(code=%d,msg=%s)", rc, error_msg);
			if(rc == 2003 || rc == 2004 || rc == 2054 || rc == 2055){ 
				//require reconnect
				kcxpcli_destroy(&kcbp); 
			} 
		}  

destroy:
		json_destroy(json);
		msg_destroy(&res);

	}
	return NULL;
}



void* target2zbus(void* args){
	rclient_t* client; 
	client = rclient_connect(g_proxy_cfg->zbus_broker, g_proxy_cfg->zbus_reconnect_timeout); 

	char error_code[1024] = {0};
	char field_value[1024] = {0};
	char column_name[1024] = {0}; 
	char debug_info[1024] = {0};
	char error_msg[1024] = {0}; 

	tagCallCtrl bpctrl;
	int rc;

	char mq_reply[1024] = {0};
	char msgid[1024] = {0};

	void* kcbp = kcxpcli_new(g_proxy_cfg); 
	while(1){
		while(!kcbp){ //assume target ok, if not reconnect
			zlog("KCXP(%s:%d) down, reconnecting...", g_proxy_cfg->target_host,g_proxy_cfg->target_port);
			Sleep(g_proxy_cfg->target_reconnect_timeout);
			kcbp = kcxpcli_new(g_proxy_cfg);
		}
		memset(&bpctrl, 0, sizeof(bpctrl));

		int expiry = g_proxy_cfg->probe_interval/1000;
		if(expiry == 0) expiry = 10;

		bpctrl.nExpiry = expiry;
		strcpy(bpctrl.szMsgId,"0");
		strcpy(bpctrl.szCorrId, g_proxy_cfg->target_name);
		KCBPCLI_BeginWrite(kcbp);
		rc = KCBPCLI_GetReply(kcbp, &bpctrl);

		if( rc != 0){
			if(rc == 2003 || rc == 2004 || rc == 2054 || rc == 2055){ 
				KCBPCLI_GetErrorMsg(kcbp, error_msg);
				zlog("WARN: KCXP failed(code=%d,msg=%s)", rc, error_msg);
				//require reconnect
				kcxpcli_destroy(&kcbp); 
			} 
			rclient_probe(client);
			continue;
		} 
		
		
		int parse_res = zbus_route_parse(bpctrl.szMsgId, mq_reply, msgid);
		
		if(parse_res == -1){ 
			continue;
		}

		json_t* reply = json_object(); 
		int cols = 0;
		rc = KCBPCLI_SQLNumResultCols( kcbp, &cols );
		if(cols < 3){ 
			kcxpcli_clear_data(kcbp); 
			sprintf(error_code, "%d", rc); 
			json_object_addstr(reply, "error_code", error_code);
		} else {
			KCBPCLI_SQLFetch( kcbp );  
			KCBPCLI_RsGetCol( kcbp, 1, field_value);
			KCBPCLI_RsGetCol( kcbp, 2, error_code);
			KCBPCLI_RsGetCol( kcbp, 3, field_value);
			
			if(cols == 4)
				KCBPCLI_RsGetCol( kcbp, 4, debug_info); 

			if(atoi(error_code) != 0){ 
				json_object_addstr(reply, "error_code", error_code);
				json_object_addstr(reply, "error_msg", field_value);
				if(g_proxy_cfg->verbose){
					zlog("KCXP=>ZBUS(MsgID=%s) 应答: 错误码=%s,错误消息=%s", msgid, error_code, field_value);
				}
			} else { 
				json_t* rs_array = json_array();
				int rs_count = 0;
				if(g_proxy_cfg->verbose){
					zlog("KCXP=>ZBUS(MsgID=%s) 应答:", msgid);
				}
				while(KCBPCLI_SQLMoreResults(kcbp) == 0){
					rs_count++; 
					int cols;
					int rc = KCBPCLI_SQLNumResultCols(kcbp, &cols);
					if(rc != 0) continue; 

					int rows = 0;
					if(g_proxy_cfg->verbose){ zlog_raw("结果集[%d]\n", rs_count);}
					json_t* json_rs = json_array();
					while(KCBPCLI_SQLFetch(kcbp) == 0){
						json_t* json_row = json_object();
						rows++;
						if(g_proxy_cfg->verbose){ zlog_raw("结果集[%d][行%d]: ", rs_count, rows); }
						for( int i=1; i<=cols; i++){ //假设column不重名
							unsigned char* col_val;
							long  col_len;
							KCBPCLI_RsGetColName(kcbp, i, column_name, 1024);
							KCBPCLI_RsGetVal(kcbp, i, &col_val, &col_len); 
							json_t* json_val = json_base64str(col_val, col_len);
							json_object_add(json_row, column_name, json_val); 

							if(g_proxy_cfg->verbose){
								const int max_len = 1024;
								char val[max_len+1] = {0};
								if(col_len>max_len){
									strcpy(val, "{大数据块/省略}");
								} else {
									strncpy(val, (char*)col_val, col_len);
									val[col_len] = '\0';
								}
								zlog_raw("[%s=%s] ", column_name, val);
							}
						} 
						json_array_add(json_rs, json_row); //行结束
						if(g_proxy_cfg->verbose){  zlog_raw("\n"); }
					}
					json_array_add(rs_array, json_rs);//结果集结束
				} 
				json_object_add(reply, "result", rs_array);
			}
		}

		char* res_body_str = json_dump(reply);
		json_destroy(reply);

		msg_t* msg = msg_new(); 
		msg_set_command(msg, PRODUCE);
		msg_set_mq(msg, mq_reply);
		msg_set_msgid(msg, msgid);
		msg_set_body_nocopy(msg, res_body_str, strlen(res_body_str));

		rclient_send(client, msg); 
	}

	return NULL;
}

int main(int argc, char *argv[]){
	g_proxy_cfg = proxy_cfg_new(argc, argv); 

	if(g_proxy_cfg->log_path){
		zlog_set_file(g_proxy_cfg->log_path); 
	} else {
		zlog_set_stdout();
	}

	g_mutex_target2zbus = (pthread_mutex_t*)malloc(sizeof(pthread_mutex_t));
	pthread_mutex_init(g_mutex_target2zbus);
	g_msgid_target2zbus = hash_new(&g_hctrl_msgid_target2zbus, NULL);
	


	char* broker = strdup(g_proxy_cfg->zbus_broker);

	int thread_count = g_proxy_cfg->worker_threads;
	pthread_t* zbus2target_threads = (pthread_t*)malloc(thread_count*sizeof(pthread_t));
	pthread_t* target2zbus_threads = (pthread_t*)malloc(thread_count*sizeof(pthread_t));

	for(int i=0; i<thread_count; i++){
		pthread_create(&zbus2target_threads[i], NULL, zbus2target, NULL); 
		pthread_create(&target2zbus_threads[i], NULL, target2zbus, NULL); 
	} 

	free(broker);

	for(int i=0; i<thread_count; i++){
		pthread_join(&zbus2target_threads[i], NULL);
		pthread_join(&target2zbus_threads[i], NULL);
	}  

	proxy_cfg_destroy(&g_proxy_cfg);

	pthread_mutex_destroy(g_mutex_target2zbus);
	free(g_mutex_target2zbus);
	hash_destroy(&g_msgid_target2zbus);

	getchar();
	return 0;
}