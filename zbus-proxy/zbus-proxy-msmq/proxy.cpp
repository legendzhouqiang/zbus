#include "platform.h" 
#include "log.h"
#include "thread.h"
#include "zbus.h"
#include "blockq.h"
#include "hash.h"
#include "list.h"


#include "crypt.h"


#import "mqoa.dll"
using namespace MSMQ;


const char* g_sn_delimit = "~_*&#^#&*_~"; //may define your own



static IMSMQQueuePtr msmq_open(IMSMQQueueInfoPtr qinfo, enum MQACCESS access){
	IMSMQQueuePtr queue;
	try{
		queue = qinfo->Open(access, MQ_DENY_NONE);
	}catch(_com_error& e){
		wprintf(L"Error Code = 0x%X\nError Description = %s\n", e.Error(), (wchar_t *)e.Description());
		try{
			_variant_t IsTransactional(false);
			_variant_t IsWorldReadable(true);
			qinfo->Create(&IsTransactional, &IsWorldReadable);
			queue = msmq_open(qinfo , access);
		}catch (_com_error& e){ 
			if(e.Error() == 0xC00E0005){ //already exists
				return msmq_open(qinfo , access);
			}
			wprintf(L"Error Code = 0x%X\nError Description = %s\n", e.Error(), (wchar_t *)e.Description());
			assert(0);
		} 
	}
	return queue;
}


static void msmq_close(IMSMQQueuePtr queue){
	queue->Close(); 
}


static int msmq_send(IMSMQQueuePtr queue, char* msg, int timeout){
	IMSMQMessagePtr pmsg("MSMQ.MSMQMessage");  
	pmsg->Body = msg;
	pmsg->MaxTimeToReachQueue = timeout;
	pmsg->MaxTimeToReceive = timeout;
	try{
		pmsg->Send(queue);
	}catch(_com_error& e){ 
		wprintf(L"Error Code = 0x%X\nError Description = %s\n", e.Error(), (wchar_t *)e.Description());
		return -1;
	} 
	return 0;
}


static IMSMQMessagePtr msmq_recv(IMSMQQueuePtr queue, int timeout){
	IMSMQMessagePtr	pmsg("MSMQ.MSMQMessage");
	_variant_t	vtimeout((long)timeout); 
	_variant_t  want_body((bool)true); 
	pmsg = queue->Receive(&vtMissing, &vtMissing, &want_body, &vtimeout);
	return pmsg;
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


typedef struct _proxy_cfg_t{ 
	char* msmq_server;
	char* msmq_client; 
	int	  msmq_timeout;
	int   zbus_timeout;
	int   zbus_reconnect_timeout;

	int   worker_threads;
	int   verbose;
	int	  debug;
	char* service_name; 
	char* service_regtoken;
	char* service_acctoken;
	char* brokers;

	char* log_path;
} proxy_cfg_t;
  


hash_ctrl_t hash_ctrl_str_blockq = {
	hash_func_string,			/* hash function */
	hash_dup_string,			/* key dup */
	NULL,				        /* val dup */
	hash_cmp_string,			/* key compare */
	hash_destroy_string,		/* key destructor */
	NULL,			            /* val destructor */
};

hash_t* g_blockq_map;
proxy_cfg_t*		g_proxy_cfg;
zlog_t* g_log;

proxy_cfg_t*
proxy_cfg_new(int argc, char* argv[]){
	proxy_cfg_t* self = (proxy_cfg_t*)malloc(sizeof(proxy_cfg_t));
	assert(self);
	memset(self, 0, sizeof(proxy_cfg_t));

	self->verbose = atoi(option(argc, argv, "-v", "1"));
	self->debug = atoi(option(argc, argv, "-dbg", "0"));
	self->msmq_server = strdup(option(argc, argv, "-msmq_s", "127.0.0.1"));
	self->msmq_client = strdup(option(argc, argv, "-msmq_c", "127.0.0.1"));
	self->msmq_timeout = atoi(option(argc, argv, "-msmq_t", "10000")); 
	self->zbus_timeout = atoi(option(argc, argv, "-zbus_t", "10000"));
	self->zbus_reconnect_timeout = atoi(option(argc, argv, "-zbus_r", "3000")); 
	self->worker_threads = atoi(option(argc, argv, "-c", "1"));
	self->service_name = strdup(option(argc, argv, "-s", "Trade"));
	self->service_regtoken = strdup(option(argc,argv, "-kreg", ""));
	self->service_acctoken = strdup(option(argc,argv, "-kacc", ""));
	self->brokers = strdup(option(argc,argv, "-b", "10.8.30.4:15555;10.8.30.4:15556"));
	self->log_path = strdup(option(argc,argv, "-log", "logs"));

	return self;
}

void
proxy_cfg_destroy(proxy_cfg_t** self_p){
	assert(self_p);
	proxy_cfg_t* self = *self_p;
	if(self){
		if(self->msmq_client)
			free(self->msmq_client);
		if(self->msmq_server)
			free(self->msmq_server);
		if(self->service_name)
			free(self->service_name);
		if(self->brokers)
			free(self->brokers);
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


void* zbus2target(void* args){
	char* broker = (char*)args;

	rclient_t* client;
	consumer_t* zbus_consumer; 

	client = rclient_connect(broker, g_proxy_cfg->zbus_reconnect_timeout);
	zbus_consumer = consumer_new(client, g_proxy_cfg->service_name, MODE_MQ);
	consumer_set_acc_token(zbus_consumer, g_proxy_cfg->service_acctoken);
	consumer_set_reg_token(zbus_consumer, g_proxy_cfg->service_regtoken);
	
	
	char* client_ip = g_proxy_cfg->msmq_client;
	char* server_ip = g_proxy_cfg->msmq_server;

	char client_ip_[100];
	sprintf(client_ip_, "%s", client_ip); 
	for(int i=0;i<strlen(client_ip_);i++) if(client_ip_[i] == '.') client_ip_[i] = '_';

	char msmq_name[256];
	sprintf(msmq_name, "DIRECT=TCP:%s\\PRIVATE$\\%s_recv", server_ip, client_ip_);
	zlog(g_log, "zbus2msmq: %s", msmq_name);  
	
	while(1){ 

		IMSMQQueueInfoPtr qinfo = IMSMQQueueInfoPtr("MSMQ.MSMQQueueInfo");
		qinfo->FormatName = msmq_name;
		qinfo->Label = msmq_name;
		IMSMQQueuePtr msmq_producer = msmq_open(qinfo, MQ_SEND_ACCESS); 

		while(1){
			msg_t* req = NULL;
			int rc;
			char msgid_prefix[256], *msmq_req;
			int msmq_req_len;

			rc = consumer_recv(zbus_consumer, &req, g_proxy_cfg->zbus_timeout);
			if(rc < 0) continue;
			if( !req ) continue; 
			
			//=============增加支持加解密的服务=============== 
			if(msg_get_head_or_param(req, DO_CRYPT)){ 
				msg_t* resp = crypt_handler(req); 
				consumer_reply(zbus_consumer, resp);
				continue;;
			}
			
			char* from_broker = msg_get_head_or_param(req, HEADER_BROKER);
			char* mq_reply = msg_get_mq_reply(req);
			char* msgid = msg_get_msgid_raw(req);
			sprintf(msgid_prefix, "%s,%s,%s%s", from_broker, mq_reply, msgid, g_sn_delimit);
			int body_len = msg_get_body_len(req);
			void* body = msg_get_body(req); 
		
			msmq_req_len = strlen(msgid_prefix) + body_len + 1;
			msmq_req = (char*)malloc(msmq_req_len);
			memset(msmq_req, 0, msmq_req_len);
			strcpy(msmq_req, msgid_prefix);
			memcpy(msmq_req+strlen(msgid_prefix), body, body_len); 

			msg_destroy(&req); //destroy msg

			if(g_proxy_cfg->verbose){
				zlog(g_log, "[MSMQ] REQ: %s\n", msmq_req);
			} 

			rc = msmq_send(msmq_producer, msmq_req, g_proxy_cfg->msmq_timeout);
			free(msmq_req);
			if(rc != 0){
				break;
			}
		}
		msmq_close(msmq_producer);
	}
	return NULL;
}

static int s_parse_head(char* head, char* broker, char* mq_reply, char* msgid, char* error){ 
	char* p = strstr(head, ",");
	if(p == NULL){
		strcpy(error, "[ERROR]: MSMQ invalid message, missing broker\n");
		return -1;
	}
	strncpy(broker, head, p-head);
	broker[p-head] = '\0';
	head = p+1;
	p = strstr(head, ",");
	if(p == NULL){
		strcpy(error, "[ERROR]: MSMQ invalid message, missing mq_reply\n");
		return -1;
	}
	strncpy(mq_reply, head, p-head);
	mq_reply[p-head] = '\0';

	head = p+1;
	strcpy(msgid, head);

	return 0;
}


void* target2blockq(void* args){  
	char* client_ip = g_proxy_cfg->msmq_client;
	char client_ip_[100];
	sprintf(client_ip_, "%s", client_ip); 
	for(int i=0;i<strlen(client_ip_);i++) if(client_ip_[i] == '.') client_ip_[i] = '_';

	char msmq_name[256];
	sprintf(msmq_name, ".\\PRIVATE$\\%s_send", client_ip_);
	zlog(g_log, "msmq2blockq: %s", msmq_name);   
	IMSMQQueuePtr msmq_consumer = NULL;
	while(1){
		try{   
			IMSMQQueueInfoPtr qinfo = IMSMQQueueInfoPtr("MSMQ.MSMQQueueInfo");
			qinfo->PathName = msmq_name;
			qinfo->Label = msmq_name;
			msmq_consumer = msmq_open(qinfo, MQ_RECEIVE_ACCESS);  

			IMSMQMessagePtr	pmsg("MSMQ.MSMQMessage");
			_variant_t	timeout((long)g_proxy_cfg->msmq_timeout); 
			_variant_t  want_body((bool)true); 
			int rc;

			while(1){   
				pmsg = msmq_consumer->Receive(&vtMissing, &vtMissing, &want_body, &timeout);
				if(pmsg == NULL){ 
					continue;   
				} 
				_bstr_t body = pmsg->Body;  

				char* msmq_msg = (char*)body;  
				if(g_proxy_cfg->verbose){
					zlog(g_log, "[MSMQ] REP: %s\n", msmq_msg);
				}
				char* split = strstr(msmq_msg, g_sn_delimit);
				if(split == NULL){
					zlog(g_log, "[ERROR]: MSMQ invalid message, missing header\n");
					continue;
				}
 
				*split = '\0';
				char* msg_head_str = msmq_msg;
				char* msg_body = split+strlen(g_sn_delimit); 
				char to_broker[64], mq_reply[64], msgid[64], errormsg[128];

				rc = s_parse_head(msg_head_str, to_broker, mq_reply, msgid, errormsg);
				if(rc != 0){
					zlog(g_log, errormsg);
					continue;
				}
				blockq_t* q_send = (blockq_t*)hash_get(g_blockq_map, to_broker);
				if(q_send == NULL){ 
					zlog(g_log, "missing target zbus(%s)", to_broker);
					continue;
				}
				
				msg_t* zbusmsg = msg_new();
				msg_set_command(zbusmsg, PRODUCE);
				msg_set_mq(zbusmsg, mq_reply);
				msg_set_msgid(zbusmsg, msgid);
				msg_set_ack(zbusmsg, false);
				msg_set_body(zbusmsg, msg_body);

				blockq_push(q_send, zbusmsg);
			}
		} catch(_com_error& e){ 
			wprintf(L"Error Code = 0x%X\nError Description = %s\n", e.Error(), (wchar_t *)e.Description());
			Sleep(100); //
			msmq_close(msmq_consumer);
			zlog(g_log, "Going to retry recv from MSMQ...\n");
		} 
	}

	return NULL;
}


 
void* blockq2zbus(void* args){
	char* broker = (char*)args;
	blockq_t* q_send = (blockq_t*)hash_get(g_blockq_map, broker);
	if(q_send == NULL){
		zlog(g_log, "blockq for zbus(%s) not found", broker);
		return NULL;
	}

	rclient_t* client = rclient_connect(broker, g_proxy_cfg->zbus_reconnect_timeout);
	int rc;
	msg_t* msg;
	while(1){
		msg = (msg_t*)blockq_pop(q_send);
		rc = rclient_send(client, msg);		
		if(rc < 0){ //missing message
			rclient_reconnect(client, g_proxy_cfg->zbus_reconnect_timeout);
		}
	}
	return NULL;
}

int main(int argc, char* argv[]){   
	if(argc>1 && ( strcmp(argv[1],"help")==0 
		|| strcmp(argv[1],"--help")==0 )){
			printf("-v\tverbose mode to show message, e.g. -v0, -v1\n");
			printf("-msmq_s\t msmq server e.g. -msmq_s127.0.0.1\n");
			printf("-msmq_c\t msmq client e.g. -msmq_c127.0.0.1 \n");
			printf("-msmq_t\t msmq recv timeout e.g. -msmq_t1000 \n");
			printf("-c\tmsmq proxy thread count e.g. -c1 \n"); 
			printf("-s\t zbus service name e.g. -sTrade \n"); 
			printf("-b\tzbus broker,  e.g. -b172.24.180.27:15555 \n");
			printf("-kreg\tzbus service registration token,  e.g. -kregxyz \n");
			printf("-kacc\tzbus service access token e.g. -kaccxyz \n"); 
			printf("-log\tlog file path,  e.g. -loglogs \n");
			return 0;
	}
 
	::CoInitializeEx(NULL,COINIT_MULTITHREADED); 

	g_proxy_cfg = proxy_cfg_new(argc, argv); 

	char instance_id[512];
	sprintf(instance_id,"MSMQ_%s_%s", g_proxy_cfg->msmq_server, g_proxy_cfg->msmq_client);
	
	//printf("InstanceId=%s\n", instance_id);
	HANDLE mutex = CreateMutex(NULL, FALSE, (LPCTSTR)"zbus-msmq"); 
	if(GetLastError()==ERROR_ALREADY_EXISTS){
		printf("链接同一个MSMQ私有队列不能多实例运行,请关闭之前的实例\n");
		getchar();
		CloseHandle(mutex);
		mutex = NULL;
		::CoUninitialize();
		return -1;
	}
	
	g_log = zlog_new(g_proxy_cfg->log_path);
	g_blockq_map = hash_new(&hash_ctrl_str_blockq, NULL);

	char* brokers = strdup(g_proxy_cfg->brokers); 
	list_t* broker_list = list_new();
	char* broker = strtok(brokers,";");
	while (broker){  
		blockq_t* q = blockq_new();
		hash_put(g_blockq_map, broker, q);

		list_push_back(broker_list, strdup(broker));
		broker = strtok(NULL, ";");
	} 
	free(brokers); 

	int thread_count = g_proxy_cfg->worker_threads;
	int broker_count = list_size(broker_list);
	pthread_t* zbus2msmq_threads = (pthread_t*)malloc(broker_count*thread_count*sizeof(pthread_t));
	pthread_t* msmq2blockq_threads = (pthread_t*)malloc(broker_count*thread_count*sizeof(pthread_t));
	pthread_t* blockq2zbus_threads = (pthread_t*)malloc(broker_count*thread_count*sizeof(pthread_t));
	
	list_node_t* node = list_head(broker_list);
	int broker_idx = 0;
	while(node){
		char* broker = (char*)list_value(node); 
		int d = broker_idx*thread_count;
		for(int i=0; i<thread_count; i++){
			pthread_create(&zbus2msmq_threads[d+i],   NULL, zbus2target, broker); 
			pthread_create(&msmq2blockq_threads[d+i], NULL, target2blockq, NULL); 
			pthread_create(&blockq2zbus_threads[d+i], NULL, blockq2zbus, broker); 
		} 
		broker_idx++;
		node = list_next(node);
	}  

	for(int i=0; i<thread_count*broker_count; i++){
		pthread_join(&zbus2msmq_threads[i], NULL);
		pthread_join(&msmq2blockq_threads[i], NULL);
		pthread_join(&blockq2zbus_threads[i], NULL);
	}

	hash_destroy(&g_blockq_map);
	zlog_destroy(&g_log);

	proxy_cfg_destroy(&g_proxy_cfg);
 
	::CoUninitialize();
	return 0;
}