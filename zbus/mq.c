#include "mq.h"
#include "list.h"
#include "threading.h"
#include "logger.h"


#define ZLOG(priority, ...) do{\
	if(priority <= zlog_get_level()){\
	FILE* file = zlog_get_file();\
	zlog_head(priority);\
	fprintf(file, "%s:%d: %s(): ", __FILE__, __LINE__, __FUNCTION__);\
	fprintf(file, __VA_ARGS__);\
	fprintf(file, "\n");\
	}\
}while(0)



struct sess{
	char* sessid;
	struct bufferevent* bev;
	list_node_t* sess_list_node; //关联MQ中的位置
};

struct sess_msg{
	zmsg_t* msg;//消息引用（只有指向权）
	list_node_t* glob_msg_node; //在全局消息队列中的位置
};

struct glob_msg{
	zmsg_t* msg; //消息实体（有所有权）
	list_node_t* sess_msg_node; //在Session私有队列中的位置
	list_t* sess_msg_list; //Session私有队列引用
};

mq_t* mq_new(char* name, char* token, int mode){
	mq_t* self = malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));

	self->glob_msg_list = list_new(); 
	self->recv_msg_hash = hash_new(&hash_ctrl_copy_key_string, NULL);
	
	self->recv_list = list_new();  
	self->recv_hash = hash_new(&hash_ctrl_copy_key_string, NULL);

	self->name = strdup(name);
	self->token = strdup(token);
	self->mode = mode;
	return self;
}

void mq_destroy(mq_t** self_p){
	mq_t* self = *self_p;
	if(self){
		if(self->name){
			free(self->name);
		}
		if(self->token){
			free(self->token);
		}
		if(self->glob_msg_list){
			zmsg_t* zmsg;
			while(zmsg = list_pop_front(self->glob_msg_list)){
				zmsg_destroy(&zmsg);
			}
			list_destroy(&self->glob_msg_list);
		}
		if(self->recv_msg_hash){
			hash_destroy(&self->recv_msg_hash);
		}
		if(self->recv_hash){
			hash_destroy(&self->recv_hash);
		}
		if(self->recv_list){
			sess_t* sess;
			while(sess = list_pop_front(self->recv_list)){
				sess_destroy(&sess);
			}
			list_destroy(&self->recv_list);		
		}
		
		free(self);
		*self_p = NULL;
	}
}

void gen_default_sessid(char* default_sessid, struct bufferevent* bev){
	sprintf(default_sessid, "%d", bufferevent_getfd(bev));
}

sess_t* sess_new(char* sessid, struct bufferevent* bev){
	sess_t* self;
	char default_sessid[64];

	if(sessid == NULL){
		gen_default_sessid(default_sessid, bev);
		sessid = default_sessid;
	}

	self = malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->sessid = strdup(sessid);
	self->bev = bev;

	return self;
}
void sess_destroy(sess_t** self_p){
	sess_t* self = *self_p;
	if(self){
		if(self->sessid){
			free(self->sessid);
		}
		free(self);
		*self_p = NULL;
	}
}
 

void mq_push_msg(mq_t* self, zmsg_t* zmsg){
	list_node_t* sess_msg_node, *glob_msg_node;
	struct sess_msg* sess_msg;
	struct glob_msg* glob_msg;
	list_t* sess_msg_list;

	//1) push to global message list
	glob_msg = malloc(sizeof(*glob_msg)); 
	glob_msg->msg = zmsg; 
	glob_msg_node = list_push_back(self->glob_msg_list, glob_msg);
	
	//2) find session message list, create if not found
	sess_msg_list = hash_get(self->recv_msg_hash, zmsg->recver);
	if(sess_msg_list == NULL){
		sess_msg_list = list_new(); //no need to destroy zmsg element
		hash_put(self->recv_msg_hash, zmsg->recver, sess_msg_list);
	}

	//3) push to session message list
	sess_msg = malloc(sizeof(*sess_msg)); 
	sess_msg->msg = zmsg; 
	sess_msg_node = list_push_back(sess_msg_list, sess_msg);


	//3) link session node and global node together
	glob_msg->sess_msg_list = sess_msg_list;
	glob_msg->sess_msg_node = sess_msg_node;

	sess_msg->glob_msg_node = glob_msg_node;
}


zmsg_t* mq_fetch_msg(mq_t* self, char* recver){

	zmsg_t* zmsg = NULL;
	struct glob_msg* glob_msg;
	struct sess_msg* recv_msg;
	list_t* recv_msg_list;

	if(self->mode == MQ_ROLLER){ //roller ignore recver
		list_node_t* node = list_head(self->glob_msg_list);
		if(node){
			glob_msg = list_value(node);
			recv_msg = list_value(glob_msg->sess_msg_node);
			zmsg = glob_msg->msg;

			list_remove_node(glob_msg->sess_msg_list, glob_msg->sess_msg_node);
			list_remove_node(self->glob_msg_list, node);
			free(glob_msg);
			free(recv_msg);
		}
		return zmsg;
	}

	recv_msg_list = hash_get(self->recv_msg_hash, recver);
	if(!recv_msg_list) return NULL;
	
	recv_msg = list_pop_front(recv_msg_list);
	if(!recv_msg) return NULL;

	zmsg = recv_msg->msg; 
	glob_msg = list_value(recv_msg->glob_msg_node);
	list_remove_node(self->glob_msg_list, recv_msg->glob_msg_node);
	free(glob_msg);
	free(recv_msg);

	return zmsg;
}

void mq_put_recver(mq_t* self, char* recver, struct bufferevent* bev){
	list_node_t* node;
	struct sess* sess;
	char default_sessid[64];

	if(recver == NULL){
		gen_default_sessid(default_sessid, bev);
		recver = default_sessid;
	}
	sess = hash_get(self->recv_hash, recver);
	if(sess){ //
		return; 
	}

	sess = sess_new(recver, bev);
	hash_put(self->recv_hash, sess->sessid, sess);
	node = list_push_back(self->recv_list, sess);
	sess->sess_list_node = node;
}

sess_t* mq_next_sess(mq_t* self){
	sess_t* sess = list_pop_front(self->recv_list);
	list_node_t* node;
	if(sess){
		node = list_push_back(self->recv_list, sess); //rolling back
		sess->sess_list_node = node;
	}
	return sess;
}

sess_t* mq_get_recver(mq_t* self, char* recver){
	return hash_get(self->recv_hash, recver);
}


void mq_rem_recver(mq_t* self, char* recver){
	struct sess* sess;
	sess = hash_get(self->recv_hash, recver);
	if(sess == NULL) return;

	list_remove_node(self->recv_list, sess->sess_list_node);
	hash_rem(self->recv_hash, recver);
	sess_destroy(&sess);
}


void mq_dispatch_zmsg(mq_t* self, zmsg_t* zmsg){
	sess_t* sess = NULL; 
	struct evbuffer* output;  

	if(self->mode == MQ_ROLLER){
		sess = mq_next_sess(self);
	} else if (self->mode == MQ_MATCH){
		sess = mq_get_recver(self, zmsg->recver);
	}
	//TODO add MQ_FILTER support

	if(sess == NULL){
		mq_push_msg(self, zmsg);
		return;
	} 

	output = bufferevent_get_output(sess->bev);
	evbuffer_add_zmsg(output, zmsg);  

}

void mq_dispatch_recver(mq_t* self, char* recver){
	sess_t* sess = NULL; 
	struct evbuffer* output; 
	zmsg_t* zmsg;
	sess = mq_get_recver(self, recver);
	if(sess == NULL){
		ZLOG(LOG_WARNING, "Session(%s) Not Found", recver);
		return;
	}

	while(zmsg = mq_fetch_msg(self, recver)){
		output = bufferevent_get_output(sess->bev);
		evbuffer_add_zmsg(output, zmsg); 
	}
}

int test_mq(int argc, char *argv[])
{
	int i, total=0;
	mq_t* mq;
	zmsg_t* zmsg, *zmsg2;
	int64_t start, end;
	size_t count = 100000;
	start = current_millis();
	printf("start=%lld\n", start);
	mq = mq_new("MQ_TEST", "token", MQ_MATCH);
	for(i=0; i< count;i++){
		char sess[32], body[32];
		sprintf(sess, "SESS%03d", i%100);
		sprintf(body, "BODY%03d", i);

		zmsg = zmsg_new();
		zmsg_sender(zmsg, sess);
		//zmsg_head_set(zmsg, "key", "val");
		zmsg_body(zmsg, body);
		//zmsg_destroy(&zmsg);
		mq_push_msg(mq, zmsg);
		//zmsg_print(zmsg, stdout);
	}
	
	total = 0;
	for(i=0; i<count;i++){
		char sess[32];
		sprintf(sess, "SESS%03d", i%100);
		if(zmsg2 = mq_fetch_msg(mq, NULL)){
			//zmsg_print(zmsg2, stdout); 
			total++;
			zmsg_destroy(&zmsg2);
		} 
	}
	end = current_millis();
	printf("end  =%lld, total=%d\n", end, total);
	printf("QPS=%.2f\n", count*1000.0/(end-start));



	mq_destroy(&mq);
	printf("===ok===\n");
	getchar();
	return 0;
}