#include "platform.h" 
#include "logger.h"
#include "evzmsg.h" 
#include "mq.h"
#include "zbus.h" 

#include <event2/bufferevent.h>
#include <event2/buffer.h>
#include <event2/listener.h>
#include <event2/util.h>
#include <event2/event.h>

struct zbus{ 
	struct event_base* evbase;
	struct evconnlistener *listener;

	hash_ctrl_t* ctrl_mq;
	hash_t*	 mqs; 

	hash_ctrl_t* ctrl_recver;
	hash_t*  recvers;
	char host[32];
	int port; 
};

zbus_t* zbus = NULL;

#define ZLOG(priority, ...) do{\
	if(priority <= zlog_get_level()){\
		FILE* file = zlog_get_file();\
		zlog_head(priority);\
		fprintf(file, "%s:%d: %s(): ", __FILE__, __LINE__, __FUNCTION__);\
		fprintf(file, __VA_ARGS__);\
		fprintf(file, "\n");\
	}\
}while(0)


static int 
	zbus_init(int argc, char* argv[]);
static void 
	zbus_destroy();
static void 
	zbus_run();

static char* 
	option(int argc, char* argv[], char* opt, char* default_value);
static void 
	s_mq_destroy(void *privdata, void* ptr);
static void 
	s_recver_destroy(void *privdata, void* ptr);

struct evconnlistener *
	create_listener(struct event_base* evbase, const char* addr_str, evconnlistener_cb cb);
static void 
	zbus_accept_cb(struct evconnlistener *, evutil_socket_t, struct sockaddr *, int socklen, void *);
static void 
	zbus_write_cb(struct bufferevent *, void *);
static void 
	zbus_event_cb(struct bufferevent *, short, void *);
static void 
	zbus_read_cb(struct bufferevent *, void *); 
static zmsg_t* 
	handle_zmsg(zmsg_t* zmsg, struct bufferevent *bev); 
static void 
s_recver_destroy(void *privdata, void* ptr){
	hash_t* d = (hash_t*) ptr;
	hash_destroy(&d);
}
static int zbus_init(int argc, char* argv[]){
	hash_ctrl_t* ctrl;
	char address[64];

	if(zbus != NULL) return 0;

	zbus = malloc(sizeof(*zbus));
	memset(zbus, 0, sizeof(*zbus));
	zbus->evbase = event_base_new();

	if(!zbus->evbase){
		ZLOG(LOG_ERR, "event_base_new() failed");
		return -1;
	}

	strcpy(zbus->host, option(argc, argv, "-h", "0.0.0.0"));
	zbus->port = atoi(option(argc, argv, "-p", "15555"));
	
	ctrl = malloc(sizeof(*ctrl));
	memset(ctrl, 0, sizeof(*ctrl));
	ctrl->hash_func = hash_func_string;
	ctrl->key_dup = hash_dup_string;
	ctrl->key_cmp = hash_cmp_string;
	ctrl->key_destroy = hash_destroy_string;
	ctrl->val_destroy = s_mq_destroy;
	zbus->ctrl_mq = ctrl;

	zbus->mqs = hash_new(zbus->ctrl_mq, NULL);

	ctrl = malloc(sizeof(*ctrl));
	memset(ctrl, 0, sizeof(*ctrl));
	ctrl->hash_func = hash_func_string;
	ctrl->key_dup = hash_dup_string;
	ctrl->key_cmp = hash_cmp_string;
	ctrl->key_destroy = hash_destroy_string; 
	ctrl->val_destroy = s_recver_destroy;
	zbus->ctrl_recver = ctrl;

	zbus->recvers = hash_new(zbus->ctrl_recver, NULL);
	
	sprintf(address, "%s:%d", zbus->host, zbus->port);
	zbus->listener = create_listener(zbus->evbase, address, zbus_accept_cb); 

	if (!zbus->listener) {
		ZLOG(LOG_ERR, "Could not create listener");
		return -1;
	}  
	return 0;
}

static void zbus_destroy(){
	if(zbus == NULL) return;
	
	if(zbus->mqs){
		hash_destroy(&zbus->mqs);
	}
	if(zbus->ctrl_mq){
		free(zbus->ctrl_mq);
	}
	if(zbus->recvers){
		hash_destroy(&zbus->recvers);
	}
	if(zbus->ctrl_recver){
		free(zbus->ctrl_recver);
	}
	if(zbus->listener){
		evconnlistener_free(zbus->listener); 
	}
	if(zbus->evbase) {
		event_base_free(zbus->evbase);
	} 
}
static void zbus_run(){
	if(zbus == NULL){
		ZLOG(LOG_ERR, "zbus should be initialized first");
		return;
	}
	ZLOG(LOG_INFO, "ZBus(libevent) started on(%s:%d)", zbus->host, zbus->port);
	event_base_dispatch(zbus->evbase);
}


void s_mq_destroy(void *privdata, void* ptr){
	mq_destroy(&(mq_t*)ptr);
}

static char* option(int argc, char* argv[], char* opt, char* default_value){
	int i;
	char* value = default_value;
	for(i=1; i<argc; i++){
		if(strcmp(argv[i], opt)==0){
			if((i+1)<argc){
				value = argv[i+1];
				break;
			}
		}
	}
	return value;
}
 

//PUB
void handle_pub(mq_t* mq, zmsg_t* zmsg, struct bufferevent *bev){
	mq_dispatch_zmsg(mq, zmsg);
} 
//SUB
void handle_sub(mq_t* mq, zmsg_t* zmsg, struct bufferevent *bev){
	char* recver_id = zmsg->sender; //current bev is receiver
	hash_t* recver;
	char default_sessid[64]; 
	gen_default_sessid(default_sessid, bev);
	
	if(streq(recver_id, "")){
		recver_id = default_sessid;
	}
	
	recver = hash_get(zbus->recvers, default_sessid);
	if(recver == NULL){
		recver = hash_new(&hash_ctrl_copy_key_val_string, NULL);
		hash_put(zbus->recvers, default_sessid, recver);
	}
	hash_put(recver, recver_id, recver_id);//use hash as SET
	

	mq_put_recver(mq, recver_id, bev);
	mq_dispatch_recver(mq, recver_id);
	zmsg_destroy(&zmsg);
}



void handle_register(mq_t* mq, zmsg_t* zmsg, struct bufferevent *bev){
	char* name = zmsg->mq;
	char* token = zmsg_head_get(zmsg, "access_token");
	char* mode_str = zmsg_head_get(zmsg, "mode");
	if(mode_str==NULL) mode_str = "0";

	if(mq == NULL){
		mq = mq_new(name, token, atoi(mode_str));
		hash_put(zbus->mqs, name, mq);
	} 
	zmsg_command(zmsg, MSG);
	zmsg_status(zmsg, "200");
	zmsg_body(zmsg, "OK");
	evbuffer_add_zmsg(bufferevent_get_output(bev), zmsg);
}

void handle_hbt(mq_t* mq, zmsg_t* zmsg, struct bufferevent *bev){
	zmsg_destroy(&zmsg);
}

static void mark_session(zmsg_t* zmsg, struct bufferevent *bev){
	if(strcmp(zmsg->sender, "")==0){
		char default_sessid[64];
		sprintf(default_sessid, "%d", bufferevent_getfd(bev));
		zmsg_sender(zmsg, default_sessid);
	}
}
static void zbus_read_cb(struct bufferevent *bev, void *user_data)
{ 
	zmsg_t* zmsg; 
	mq_t* mq;  
	struct evbuffer* input = bufferevent_get_input(bev);
	while(zmsg = evbuffer_read_zmsg(input)){
		mark_session(zmsg, bev);
		zmsg_print(zmsg, stdout);

		mq = hash_get(zbus->mqs, zmsg->mq);
		
		if(strcmp(zmsg->command, REG)==0){
			handle_register(mq, zmsg, bev);
			continue;
		} else if(strcmp(zmsg->command, HBT)==0){ 
			handle_hbt(mq, zmsg, bev);
			continue;
		} 


		if( !mq ){
			char errmsg[128];
			sprintf(errmsg, "MQ(%s) Not Found", zmsg->mq);
			zmsg_command(zmsg, MSG); 
			zmsg_status(zmsg, "404");
			zmsg_body(zmsg, errmsg); 
			evbuffer_add_zmsg(bufferevent_get_output(bev), zmsg);
			continue;;
		} 

		if(strcmp(zmsg->command, PUB)==0){ 
			handle_pub(mq, zmsg, bev);

		} else if(strcmp(zmsg->command, SUB)==0){ 
			handle_sub(mq, zmsg, bev);

		} else { 
			ZLOG(LOG_WARNING, "Unknown message command=%s", zmsg->command);
			zmsg_destroy(&zmsg);
		}
	}
}



static void 
zbus_accept_cb(struct evconnlistener *listener, evutil_socket_t fd, struct sockaddr *a, int slen, void *p)
{
	struct bufferevent *bev;  
	bev = bufferevent_socket_new(zbus->evbase, fd, BEV_OPT_CLOSE_ON_FREE|BEV_OPT_DEFER_CALLBACKS);
	bufferevent_setcb(bev, zbus_read_cb, NULL, zbus_event_cb, NULL);

	bufferevent_enable(bev, EV_READ|EV_WRITE);
} 

static void s_clean_mq_recver(char* recver_id){
	hash_iter_t* it;
	hash_entry_t* e;
	it = hash_iter_new(zbus->mqs);
	while(e = hash_iter_next(it)){
		mq_t* mq = hash_entry_val(e);
		sess_t* sess = mq_get_recver(mq, recver_id);
		if(sess){
			mq_rem_recver(mq, recver_id);
			ZLOG(LOG_INFO, "Clean Receiver[%s] on MQ[%s]", recver_id, mq->name);
		} 
	}
	hash_iter_destroy(&it);
}

static void do_clean_job(struct bufferevent *bev){
	char default_sessid[64];
	hash_iter_t* it;
	hash_entry_t* e;

	hash_t* recver;
	gen_default_sessid(default_sessid, bev);
	recver = hash_get(zbus->recvers, default_sessid);
	if(recver == NULL) return;
	
	ZLOG(LOG_INFO, "Clean Socket[%s]", default_sessid);
	it = hash_iter_new(recver);
	while(e = hash_iter_next(it)){
		char* recver_id = hash_entry_key(e);
		s_clean_mq_recver(recver_id);
	}
	hash_iter_destroy(&it);

	hash_rem(zbus->recvers, default_sessid);
}

static void zbus_event_cb(struct bufferevent *bev, short events, void *user_data){
	if (events & BEV_EVENT_EOF) {
		ZLOG(LOG_INFO,"Connection closed"); 
		do_clean_job(bev);
	} else if (events & BEV_EVENT_ERROR) {
		ZLOG(LOG_ERR, "Got an error event: %d",events); 
		do_clean_job(bev);
	} else {
		ZLOG(LOG_INFO, "Event: %d", events);
	} 
}

struct evconnlistener *
create_listener(struct event_base* evbase, const char* addr_str, evconnlistener_cb cb){
	struct sockaddr_storage addr;
	struct evconnlistener *listener;
	int socklen;
	memset(&addr, 0, sizeof(addr));
	socklen = sizeof(addr);
	if (evutil_parse_sockaddr_port(addr_str,(struct sockaddr*)&addr, &socklen)<0) {
		return NULL;
	}

	listener = evconnlistener_new_bind(evbase, cb, NULL,
		LEV_OPT_CLOSE_ON_FREE|LEV_OPT_CLOSE_ON_EXEC|LEV_OPT_REUSEABLE,
		-1, (struct sockaddr*)&addr, socklen);
	return listener;
}

int main(int argc, char *argv[])
{
	int rc;
#if defined (__WINDOWS__)
	{
		WSADATA wsa_data;
		WSAStartup(0x0201, &wsa_data); 
		//evthread_use_windows_threads();
	}
#elif defined (__UNIX__)
	//evthread_use_pthreads();
#endif

	rc = zbus_init(argc, argv);
	if(rc == -1){
		ZLOG(LOG_ERR, "zbus init error");
		getchar();
		return -1;
	}

	zbus_run();
	zbus_destroy();
	printf("=======done=======");
	getchar();
	return 0;
}
