#include "platform.h" 
#include "log.h"
#include "thread.h"

#include "zbus.h"

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


typedef struct _msmq_proxy_cfg_t{ 
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
	char* broker;

	char* log_path;
} msmq_proxy_cfg_t;
  

msmq_proxy_cfg_t*		g_msmq_cfg;

msmq_proxy_cfg_t*
msmq_proxy_cfg_new(int argc, char* argv[]){
	msmq_proxy_cfg_t* self = (msmq_proxy_cfg_t*)malloc(sizeof(msmq_proxy_cfg_t));
	assert(self);
	memset(self, 0, sizeof(msmq_proxy_cfg_t));

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
	self->broker = strdup(option(argc,argv, "-b", "localhost:15555"));
	self->log_path = strdup(option(argc,argv, "-log", NULL));

	return self;
}

void
msmq_proxy_cfg_destroy(msmq_proxy_cfg_t** self_p){
	assert(self_p);
	msmq_proxy_cfg_t* self = *self_p;
	if(self){
		if(self->msmq_client)
			free(self->msmq_client);
		if(self->msmq_server)
			free(self->msmq_server);
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

void* zbus2msmq(void* args){
	rclient_t* client;
	consumer_t* zbus_consumer; 

	client = rclient_connect(g_msmq_cfg->broker, g_msmq_cfg->zbus_reconnect_timeout);
	zbus_consumer = consumer_new(client, g_msmq_cfg->service_name, MODE_MQ);
	consumer_set_acc_token(zbus_consumer, g_msmq_cfg->service_acctoken);
	consumer_set_reg_token(zbus_consumer, g_msmq_cfg->service_regtoken);
	
	
	char* client_ip = g_msmq_cfg->msmq_client;
	char* server_ip = g_msmq_cfg->msmq_server;

	char client_ip_[100];
	sprintf(client_ip_, "%s", client_ip); 
	for(int i=0;i<strlen(client_ip_);i++) if(client_ip_[i] == '.') client_ip_[i] = '_';

	char msmq_name[256];
	sprintf(msmq_name, "DIRECT=TCP:%s\\PRIVATE$\\%s_recv", server_ip, client_ip_);
	zlog("zbus2msmq: %s\n", msmq_name);  
	
	while(1){ 

		IMSMQQueueInfoPtr qinfo = IMSMQQueueInfoPtr("MSMQ.MSMQQueueInfo");
		qinfo->FormatName = msmq_name;
		qinfo->Label = msmq_name;
		IMSMQQueuePtr msmq_producer = msmq_open(qinfo, MQ_SEND_ACCESS); 

		while(1){
			msg_t* res = NULL;
			int rc;
			char msgid_prefix[256], *msmq_req;
			int msmq_req_len;

			rc = consumer_recv(zbus_consumer, &res, g_msmq_cfg->zbus_timeout);
			if(rc < 0) continue;
			if( !res ) continue;

			msg_print(res);
			
			char* from_broker = msg_get_head_or_param(res, HEADER_BROKER);
			char* mq_reply = msg_get_mq_reply(res);
			char* msgid = msg_get_msgid_raw(res);
			sprintf(msgid_prefix, "%s,%s,%s%s", from_broker, mq_reply, msgid, g_sn_delimit);
			int body_len = msg_get_body_len(res);
			void* body = msg_get_body(res); 
		
			msmq_req_len = strlen(msgid_prefix) + body_len + 1;
			msmq_req = (char*)malloc(msmq_req_len);
			memset(msmq_req, 0, msmq_req_len);
			strcpy(msmq_req, msgid_prefix);
			memcpy(msmq_req+strlen(msgid_prefix), body, body_len); 

			msg_destroy(&res); //destroy msg

			if(g_msmq_cfg->verbose){
				zlog("[MSMQ] REQ: %s\n", msmq_req);
			} 

			rc = msmq_send(msmq_producer, msmq_req, g_msmq_cfg->msmq_timeout);
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


void* msmq2zbus(void* args){ 
	rclient_t* client = rclient_connect(g_msmq_cfg->broker, 3000);
	
	char* client_ip = g_msmq_cfg->msmq_client;
	char client_ip_[100];
	sprintf(client_ip_, "%s", client_ip); 
	for(int i=0;i<strlen(client_ip_);i++) if(client_ip_[i] == '.') client_ip_[i] = '_';

	char msmq_name[256];
	sprintf(msmq_name, ".\\PRIVATE$\\%s_send", client_ip_);
	zlog("msmq2zbus: %s\n", msmq_name);   
	IMSMQQueuePtr msmq_consumer = NULL;
	while(1){
		try{   
			IMSMQQueueInfoPtr qinfo = IMSMQQueueInfoPtr("MSMQ.MSMQQueueInfo");
			qinfo->PathName = msmq_name;
			qinfo->Label = msmq_name;
			msmq_consumer = msmq_open(qinfo, MQ_RECEIVE_ACCESS);  

			IMSMQMessagePtr	pmsg("MSMQ.MSMQMessage");
			_variant_t	timeout((long)g_msmq_cfg->msmq_timeout); 
			_variant_t  want_body((bool)true); 
			int rc;

			while(1){   
				pmsg = msmq_consumer->Receive(&vtMissing, &vtMissing, &want_body, &timeout);
				if(pmsg == NULL){ 
					msg_t* zbusmsg = msg_new();
					msg_set_command(zbusmsg, HEARTBEAT);
					rc = rclient_send(client, zbusmsg);		
					if(rc < 0){ //missing message
						rclient_reconnect(client, g_msmq_cfg->zbus_reconnect_timeout);
					} 
					continue;   
				} 
				_bstr_t body = pmsg->Body;  

				char* msmq_msg = (char*)body;  
				if(g_msmq_cfg->verbose){
					zlog("[MSMQ] REP: %s\n", msmq_msg);
				}
				char* split = strstr(msmq_msg, g_sn_delimit);
				if(split == NULL){
					zlog("[ERROR]: MSMQ invalid message, missing header\n");
					continue;
				}
 
				*split = '\0';
				char* msg_head_str = msmq_msg;
				char* msg_body = split+strlen(g_sn_delimit); 
				char to_broker[64], mq_reply[64], msgid[64], errormsg[128];

				rc = s_parse_head(msg_head_str, to_broker, mq_reply, msgid, errormsg);
				if(rc != 0){
					zlog(errormsg);
					continue;
				}
				
				msg_t* zbusmsg = msg_new();
				msg_set_command(zbusmsg, PRODUCE);
				msg_set_mq(zbusmsg, mq_reply);
				msg_set_msgid(zbusmsg, msgid);
				msg_set_ack(zbusmsg, false);
				msg_set_body(zbusmsg, msg_body);

				rc = rclient_send(client, zbusmsg);		
				if(rc < 0){ //missing message
					rclient_reconnect(client, g_msmq_cfg->zbus_reconnect_timeout);
				}
			}
		} catch(_com_error& e){ 
			wprintf(L"Error Code = 0x%X\nError Description = %s\n", e.Error(), (wchar_t *)e.Description());
			Sleep(100); //
			msmq_close(msmq_consumer);
			zlog("Going to retry recv from MSMQ...\n");
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

	g_msmq_cfg = msmq_proxy_cfg_new(argc, argv); 

	if(g_msmq_cfg->log_path){
		zlog_set_file(g_msmq_cfg->log_path); 
	} else {
		zlog_set_stdout();
	}

	char* broker = strdup(g_msmq_cfg->broker);

	int thread_count = g_msmq_cfg->worker_threads;
	pthread_t* zbus2msmq_threads = (pthread_t*)malloc(thread_count*sizeof(pthread_t));
	pthread_t* msmq2zbus_threads = (pthread_t*)malloc(thread_count*sizeof(pthread_t));

	for(int i=0; i<thread_count; i++){
		pthread_create(&zbus2msmq_threads[i], NULL, zbus2msmq, NULL); 
		pthread_create(&msmq2zbus_threads[i], NULL, msmq2zbus, NULL); 
	} 

	free(broker);

	for(int i=0; i<thread_count; i++){
		pthread_join(&zbus2msmq_threads[i], NULL);
		pthread_join(&msmq2zbus_threads[i], NULL);
	}  

	msmq_proxy_cfg_destroy(&g_msmq_cfg);
 
	::CoUninitialize();
	return 0;
}