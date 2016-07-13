#include "remoting.h"
#include "hash.h"
#include "list.h"
#include "net.h"  
#include "log.h"
#include "msg.h"
#include "thread.h"
#include "buffer.h"


#ifdef __WINDOWS__
#include <objbase.h>
#else
#include <uuid/uuid.h>
typedef struct _GUID
{
	unsigned long  Data1;
	unsigned short Data2;
	unsigned short Data3;
	unsigned char  Data4[8];
} GUID, UUID;

#endif

static void gen_uuid(char* buf){
	UUID uuid;
#ifdef __WINDOWS__
	CoCreateGuid(&uuid);
#else
	uuid_generate((char*)&uuid);
#endif

sprintf(buf, "%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x",
		uuid.Data1, uuid.Data2, uuid.Data3,
		uuid.Data4[0], uuid.Data4[1],
		uuid.Data4[2], uuid.Data4[3],
		uuid.Data4[4], uuid.Data4[5],
		uuid.Data4[6], uuid.Data4[7]);
}

static void
hash_destroy_msg(void *privdata, void *key){
	msg_t* msg = (msg_t*)key;
	msg_destroy(&msg);
}

static hash_ctrl_t hash_ctrl_str11_msg01 = {
	hash_func_string,           /* hash function */
	hash_dup_string,            /* key dup */
	NULL,                       /* val dup */
	hash_cmp_string,   			/* key compare */
	hash_destroy_string,        /* key destructor */
	hash_destroy_msg            /* val destructor */
};


struct rclient{ //remoting client
	int fd;           //socket fd
	char* host;       //host ip
	int port;         //port, default to 15555  
	buf_t* rbuf;
	buf_t* wbuf;
	char*	 msgid_match;
	hash_t*  result_table; 
}; 



rclient_t* rclient_new(char* broker){
	rclient_t* self = (rclient_t*)malloc(sizeof(*self));
	char* p;
	memset(self, 0, sizeof(*self)); 
	p = strchr(broker, ':');
	if(p == NULL){
		self->host = strdup(broker);
		self->port = 15555; //default
	} else {
		self->host = (char*)malloc(p-broker+1);
		strncpy(self->host,  broker, p-broker);
		self->host[p-broker] = '\0';
		self->port = atoi(p+1);
	}
	return self;
}

rclient_t* rclient_connect(char* broker, int auto_reconnect_millis){
	rclient_t* self = rclient_new(broker);
	rclient_reconnect(self, auto_reconnect_millis);
	return self;
}



bool rclient_reconnect(rclient_t* self, int reconnect_msecs){ 
	int rc;
	if(self->fd){
		net_close(self->fd);
		self->fd = 0;
	}  
	if(self->rbuf){
		buf_destroy(&self->rbuf);
	}
	if(self->wbuf){
		buf_destroy(&self->wbuf);
	}
	if(self->msgid_match){
		free(self->msgid_match);
	}
	if(self->result_table){
		hash_destroy(&self->result_table);
	}

	self->rbuf = buf_new(2048);
	self->wbuf = NULL;
	self->result_table = hash_new(&hash_ctrl_str11_msg01, NULL);
	self->msgid_match = strdup("");

	while(rc = net_connect(&self->fd, self->host, self->port)){
		if(reconnect_msecs<=0) break; //no reconnect
		net_close(self->fd);
		printf("Try to reconnect(%s:%d) in %d ms...\n", self->host, self->port, reconnect_msecs);
		sleep(reconnect_msecs);
	}
	return rc == 0;
}


void rclient_destroy(rclient_t** self_p){
	rclient_t* self = *self_p;
	if(!self) return;   
	
	if(self->fd){
		net_close(self->fd);
	}
	if(self->rbuf){
		buf_destroy(&self->rbuf);
	}
	if(self->wbuf){
		buf_destroy(&self->wbuf);
	}
	if(self->msgid_match){
		free(self->msgid_match);
	}
	if(self->result_table){
		hash_destroy(&self->result_table);
	}

	free(self);
	*self_p = NULL;
}
void rclient_turnoff_msgid_match(rclient_t* self){
	assert(self);
	if(self->msgid_match){
		free(self->msgid_match);
		self->msgid_match = NULL;
	}
}
static void mark_msg(msg_t* msg){
	char* msgid = msg_get_id(msg);
	if(msgid == NULL){
		char buf[256];
		gen_uuid(buf);
		msg_set_id(msg, buf);
	}
} 
static void set_msgid_match(rclient_t* client, char* msgid){ 
	if(client->msgid_match){
		free(client->msgid_match);
	}
	client->msgid_match = strdup(msgid);
}

static int 
rclient_write(rclient_t* self, int* done){
	int n, len; 
	char* begin; 
	if(self->wbuf == NULL){
		if(done) *done = 1;
		return 0;
	} 

	begin = buf_begin(self->wbuf);
	len = buf_remaining(self->wbuf);
	n = net_send(self->fd, (const unsigned char *)begin, len);
	
	if(n == len){
		buf_destroy(&self->wbuf);
		if(done) *done = 1;
		return n;
	} 

	if( n > 0 ){ 
		buf_drain(self->wbuf, n);  
		if(done) *done = 0;
		return n;
	}

	if(done) *done = 0;
	return n;
}


int rclient_read(rclient_t* self, msg_t** msg_p, int timeout){
	char buf[1024*4];
	int n;
	msg_t* exit_msg;
	assert(msg_p);
	*msg_p = NULL; //assume nothing

	if(self->msgid_match){
		exit_msg = (msg_t*)hash_get(self->result_table, self->msgid_match);
		if(exit_msg){
			*msg_p = exit_msg; 
			return 0;
		}
	}

	n = net_set_timeout(self->fd, timeout);
	if( n < 0 ){ 
		return n;
	}
	n = net_recv(self->fd, (unsigned char *)buf, sizeof(buf));
	if( n>=0 ){
		msg_t* msg;
		buf_t* dup;
		int mv = 0;
		buf_put(self->rbuf, buf, n); 

		dup = buf_dup(self->rbuf); //shallow copy
		buf_flip(dup);
		msg = msg_decode(dup);
		mv = dup->position;
		buf_destroy(&dup);
		
		if(msg){
			char* msgid = msg_get_id(msg);
			if(self->msgid_match && msgid && strcmp(msgid, self->msgid_match)!=0){
				hash_put(self->result_table, msgid, msg);
			} else {
				*msg_p = msg;
			} 
			buf_mv(self->rbuf, mv);
			return n;
		}
	}
	return n; //错误码，或者消息部分读取长度
}
int rclient_send(rclient_t* self, msg_t* msg){ 
	int rc = rclient_send_try(self, msg);
	msg_destroy(&msg);
	return rc;
}

int rclient_send_try(rclient_t* self, msg_t* msg){ 
	int rc = 0; 
	int done;
	char* msgid;
	buf_t* buf = buf_new(1024);
	mark_msg(msg);
	msgid = msg_get_id(msg);
	if(msgid){
		set_msgid_match(self, msgid);
	}
	msg_encode(msg, buf); 
	//buf_print(buf);
	buf_flip(buf);

	if(self->wbuf==NULL){
		self->wbuf = buf;
	} else {
		buf_putbuf(self->wbuf, buf);
	} 
	do{
		rc = rclient_write(self, &done);
		if(rc < 0){
			return rc;
		}
	}while(!done);

	return rc; 
}

int rclient_recv(rclient_t* self, msg_t** msg_p, int timeout){
	int rc = 0;
	do {
		rc = rclient_read(self, msg_p, timeout);
		if(rc < 0){ 
			return rc;
		}
	} while (*msg_p == NULL);
	return rc;
}

int rclient_invoke(rclient_t* self, msg_t* req, msg_t** res_p, int timeout){
	int rc;
	assert(req);
	assert(res_p);
	rc = rclient_send(self, req);
	if(rc < 0){
		if(res_p) *res_p = NULL;
		return rc;
	}
	rc = rclient_recv(self, res_p, timeout);
	return rc;
}
int rclient_probe(rclient_t* self){
	msg_t* msg = msg_new();
	msg_set_cmd(msg, HEARTBEAT);
	return rclient_send(self, msg);
}

int test_remoting(int argc, char* argv[]){
	int i;
	int rc;
	rclient_t* client;
	msg_t* msg;

	client = rclient_connect("127.0.0.1:15555", 10000); 

	for(i=0;i<100000;i++){
		msg = msg_new();
		msg_set_cmd(msg, "produce");
		msg_set_mq(msg, "MyMQ");
		msg_set_bodyfmt(msg, "hello world %lld", current_millis());

		rc = rclient_send(client, msg);
		rc = rclient_recv(client, &msg, 10000);
		if(msg){
			msg_print(msg);
			msg_destroy(&msg);
		}
	}

	printf("%d\n=done=", rc);

	getchar();
	rclient_destroy(&client);
	return 0;
}
