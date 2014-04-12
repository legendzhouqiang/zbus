#include "platform.h"
#include "evzmsg.h" 
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

#define _strdup(s) ((s)? strdup(s) : NULL)


zmsg_t* zmsg_new(){
	zmsg_t* self = malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	return self;
}

void zmsg_destroy(zmsg_t** self_p){
	zmsg_t* self; 
	self = *self_p;
	if(!self) return;
	
	if(self->sender){
		free(self->sender);
	}
	if(self->recver){
		free(self->recver);
	}
	if(self->msgid){
		free(self->msgid);
	}
	if(self->mq){
		free(self->mq);
	}
	
	if(self->token){
		free(self->token);
	}
	if(self->command){
		free(self->command);
	}
	if(self->status){
		free(self->status);
	}
	
	if(self->head){
		free(self->head);
	}
	if(self->body){
		free(self->body);
	}

	if(self->head_kvs){
		hash_destroy(&self->head_kvs);
	}

	free(self);
	
	*self_p = NULL;
}

void zmsg_sender(zmsg_t* self, char* src_id){
	assert(self);
	if(self->sender){
		free(self->sender);
	}
	self->sender = _strdup(src_id);
}

void zmsg_recver(zmsg_t* self, char* dst_id){
	assert(self);
	if(self->recver){
		free(self->recver);
	}
	self->recver = _strdup(dst_id);
}

void zmsg_msgid(zmsg_t* self, char* msg_id){
	assert(self);
	if(self->msgid){
		free(self->msgid);
	}
	self->msgid = _strdup(msg_id);
}
void zmsg_command(zmsg_t* self, char* command){
	assert(self);
	if(self->command){
		free(self->command);
	}
	self->command = _strdup(command);
}

void zmsg_mq(zmsg_t* self, char* mailbox){
	assert(self);
	if(self->mq){
		free(self->mq);
	}
	self->mq = _strdup(mailbox);
}

void zmsg_token(zmsg_t* self, char* token){
	assert(self);
	if(self->token){
		free(self->token);
	}
	self->token = _strdup(token);
}

void zmsg_status(zmsg_t* self, char* status){
	assert(self);
	if(self->status){
		free(self->status);
	}
	self->status = _strdup(status);
}


/* 不能转换的string则返回NULL */
static hash_t* to_hash(char* s){
	hash_t* d = NULL;
	char* p, *p2;
	const size_t split_len = strlen(HEAD_SPLIT);
	if(s == NULL) return d;

	p2 = s;
	while(p = strstr(p2, HEAD_SPLIT)){ 
		char* eq = p2; 
		while(eq<p){
			if(*eq == '=') break;
			eq++;
		}
		if( *eq == '=' ){ 
			*eq = '\0';
			*p = '\0'; 
			if(d == NULL){
				d = hash_new(&hash_ctrl_copy_key_val_string, NULL);
			}
			hash_put(d, p2, eq+1);
			*eq = '=';
			*p = *HEAD_SPLIT;
		}
		p2 = p + split_len;
	}

	return d;
}

static char* to_string(hash_t* d){
	char* s, *p;
	size_t len = 0;
	hash_iter_t* it;
	hash_entry_t* e;
	const size_t split_len = strlen(HEAD_SPLIT);

	it = hash_iter_new(d);
	while(e = hash_iter_next(it)){
		len += strlen(hash_entry_key(e));
		len += 1+split_len;
		len += strlen(hash_entry_val(e)); 
	}
	hash_iter_destroy(&it);
	//空的hash返回NULL
	if(len == 0) return NULL;
	s = malloc(len+1); 
	p = s;
	it = hash_iter_new(d);
	while(e = hash_iter_next(it)){
		size_t size;
		size = strlen(hash_entry_key(e));
		memcpy(p,hash_entry_key(e), size); 
		p += size; 
		memcpy(p, "=", 1);
		p += 1;
		size = strlen(hash_entry_val(e));
		memcpy(p,hash_entry_val(e), size); 
		p += size;
		memcpy(p,HEAD_SPLIT, split_len); 
		p += split_len;
	}
	hash_iter_destroy(&it);
	s[len] = 0;
	return s;
}

char* zmsg_head_str(zmsg_t* self){
	if(self->head_kvs){
		if(self->head) free(self->head);
		self->head = to_string(self->head_kvs); 
		hash_destroy(&self->head_kvs);
		self->head_kvs = NULL;
	}
	return self->head;
}

void zmsg_head_set(zmsg_t* self, char* key, char* val){
	if(self->head){
		self->head_kvs = to_hash(self->head);
		if(self->head_kvs){ //准确转换
			free(self->head);
			self->head = NULL;
		} else {
			ZLOG(LOG_WARNING, "(%s) convert to hash failed", self->head);
		}
	}

	if(!self->head_kvs){
		self->head_kvs = hash_new(&hash_ctrl_copy_key_val_string, NULL);
	}
	hash_put(self->head_kvs, key, val);
}

char* zmsg_head_get(zmsg_t* self, char* key){
	if(self->head){
		self->head_kvs = to_hash(self->head);
		free(self->head);
		self->head = NULL;
	}

	if(self->head_kvs == NULL) return NULL;
	return hash_get(self->head_kvs, key);
}

void zmsg_body_blob(zmsg_t* self, void* body, size_t size){
	assert(self);
	if(self->body){
		free(self->body);
	}
	if(!body){
		self->body = NULL;
		self->body_size = 0;
		return;
	}
	self->body = malloc(size);
	self->body_size = size;
	memcpy(self->body, body, size);
}
void zmsg_body(zmsg_t* self, char* body){
	zmsg_body_blob(self, body, strlen(body));
}
void zmsg_body_nocopy(zmsg_t* self, void* body, size_t size){
	if(self->body){
		free(self->body);
	}
	self->body = body;
	self->body_size = size;
}

static void _fprintf(FILE* file, const char* fmt, char* name, char* s){
	size_t len = (s==NULL? 0 : strlen(s));
	fprintf(file, fmt, name, len, s);
}

static void _fprintf_escape(FILE* file, const char* fmt, char* name, char* s){
	const char* escape = "\r\n\r\n"; 
	char* s2;
	char* p,*p2; 
	size_t len = (s==NULL? 0 : strlen(s));
	if(len == 0){
		fprintf(file, fmt, name, len, s);
		return;
	}

	len = strlen(s); 
	s2 = malloc(len+1);
	memset(s2, 0, len+1);
	p2 = s; 
	
	while(p = strstr(p2, escape)){ 
		strncat(s2, p2, p-p2);
		if((p+4-s)<len){
			strcat(s2, "&&");
		} 
		p2 = p+4;
	}
	if( p2==s ){
		strcat(s2, p2);
	} 

	fprintf(file, fmt, name, len, s2);
	
	free(s2);
}

void zmsg_print(zmsg_t* self, FILE* file){
	char* buf;
	const char* fmt = "[%7s %06d] = %s\n";
	const char* fmt_body = "[%7s %06d] = %s\n";
	fprintf(file,"------------------------------\n");
	_fprintf(file, fmt, "SENDER", self->sender);
	_fprintf(file, fmt, "RECVER", self->recver);
	_fprintf(file, fmt, "MSGID", self->msgid);
	_fprintf(file, fmt, "MQ", self->mq);
	_fprintf(file, fmt, "TOKEN", self->token);
	_fprintf(file, fmt, "COMMAND", self->command);
	_fprintf(file, fmt, "STATUS", self->status);
	_fprintf_escape(file, fmt, "HEAD", zmsg_head_str(self));
	if(self->body && self->body_size<10240){
		buf = (char*)malloc(self->body_size+1);
		memcpy(buf,self->body,self->body_size);
		buf[self->body_size] = 0;
		fprintf(file,fmt_body,"BODY", self->body_size, buf); 
		free(buf);
	} else {
		if(!self->body){
			fprintf(file,fmt_body, "BODY", self->body_size, self->body); 
		} else {
			fprintf(file,fmt_body, "BODY", self->body_size, "{Large Blob}");
		}
	}
}

static void evbuffer_add_int(struct evbuffer* buf, int value){
	ev_uint8_t data[4];

	data[0] = (ev_uint8_t)(value>>24);
	data[1] = (ev_uint8_t)(value>>16);
	data[2] = (ev_uint8_t)(value>> 8);
	data[3] = (ev_uint8_t)(value    );

	evbuffer_add(buf, data, 4);
}

static int evbuffer_copy_int(struct evbuffer* buf){
	ev_uint8_t data[4];
	int value = 0;
	assert(evbuffer_get_length(buf)>=4);
	evbuffer_copyout(buf, data, 4);
	return  ( ((data[0])<<24) | ((data[1])<<16) | ((data[2])<<8) | (data[3]) ) ;
}

void evbuffer_add_frame(struct  evbuffer* buf, frame_t* frame){
	evbuffer_add_int(buf, frame_size(frame));
	evbuffer_add(buf, frame_data(frame), frame_size(frame));
	frame_destroy(&frame);

}

frame_t* evbuffer_read_frame(struct  evbuffer* buf){
	frame_t* frame = NULL;
	size_t buff_len, frame_size;
	void* data;

	buff_len = evbuffer_get_length(buf);
	if(buff_len<4) return NULL;

	frame_size = evbuffer_copy_int(buf); 
	if(buff_len<(frame_size+4)) return NULL;

	evbuffer_drain(buf, 4);
	data = malloc(frame_size);
	evbuffer_remove(buf, data, frame_size);

	frame = frame_new_nocopy(data, frame_size);
	return frame;
}

void evbuffer_add_msg(struct evbuffer* buf, msg_t* msg){
	size_t len; 
	frame_t* frame;
	len = msg_frame_size(msg)*4 + msg_content_size(msg);
	evbuffer_add_int(buf, len); 
	while(frame=msg_pop_front(msg)){
		evbuffer_add_frame(buf, frame);
	}
	msg_destroy(&msg);
}

void evbuffer_add_zmsg(struct evbuffer* buf, zmsg_t* zmsg){
	msg_t* msg;
	msg = to_msg(zmsg);
	evbuffer_add_msg(buf, msg);
}

msg_t* evbuffer_read_msg(struct evbuffer* buf){
	msg_t* msg; 
	size_t buff_len, msg_size, cur_msg_size;
	buff_len = evbuffer_get_length(buf);
	if(buff_len<4) return NULL;

	msg_size = evbuffer_copy_int(buf);
	if(buff_len<(msg_size+4)) return NULL;

	evbuffer_drain(buf, 4);
	
	msg = msg_new(); 
	cur_msg_size = 0;
	while(cur_msg_size<msg_size){
		frame_t* frame = evbuffer_read_frame(buf);
		if(frame==NULL){
			ZLOG(LOG_ERR, "frame is null for unpacked msg");
			break;
		}
		cur_msg_size += (4+frame_size(frame));
		msg_push_back(msg, frame);
	}
	return msg;
}

zmsg_t* evbuffer_read_zmsg(struct evbuffer* buf){
	msg_t* msg = evbuffer_read_msg(buf);
	return to_zmsg(msg);
}

static frame_t* _to_frame(char* data){ 
	size_t len = data? strlen(data) : 0;
	return frame_new_nocopy(data, len);
}

msg_t* to_msg(zmsg_t* zmsg){
	msg_t* msg = NULL; 
	if(zmsg == NULL) return NULL;
	msg = msg_new();
	msg_push_back(msg, _to_frame(zmsg->sender));
	msg_push_back(msg, _to_frame(zmsg->recver));
	msg_push_back(msg, _to_frame(zmsg->msgid));
	msg_push_back(msg, _to_frame(zmsg->mq));
	msg_push_back(msg, _to_frame(zmsg->token));
	msg_push_back(msg, _to_frame(zmsg->command));
	msg_push_back(msg, _to_frame(zmsg->status)); 
	msg_push_back(msg, _to_frame(zmsg_head_str(zmsg))); 
	msg_push_back(msg, frame_new_nocopy(zmsg->body, zmsg->body_size));

	free(zmsg);
	return msg;
}

zmsg_t* to_zmsg(msg_t* msg){
	zmsg_t* zmsg = NULL; 
	frame_t* frame;
	size_t len;

	if(msg == NULL) return NULL;

	len = msg_frame_size(msg);
	if(len < 9){
		zlog_error("zmsg frame count=%d, should >= 9\n", len);
		return NULL;
	}
	zmsg = zmsg_new();

	frame = msg_pop_front(msg);
	zmsg->sender = frame_strdup(frame);
	frame_destroy(&frame);

	frame = msg_pop_front(msg);
	zmsg->recver = frame_strdup(frame);
	frame_destroy(&frame);

	frame = msg_pop_front(msg);
	zmsg->msgid = frame_strdup(frame);
	frame_destroy(&frame);

	frame = msg_pop_front(msg);
	zmsg->mq = frame_strdup(frame);
	frame_destroy(&frame);

	frame = msg_pop_front(msg);
	zmsg->token = frame_strdup(frame);
	frame_destroy(&frame);

	frame = msg_pop_front(msg);
	zmsg->command = frame_strdup(frame);
	frame_destroy(&frame);

	frame = msg_pop_front(msg);
	zmsg->status = frame_strdup(frame);
	frame_destroy(&frame);

	frame = msg_pop_front(msg);
	zmsg->head = frame_strdup(frame);
	frame_destroy(&frame);

	frame = msg_pop_front(msg);
	zmsg->body = frame_data(frame);
	zmsg->body_size = frame_size(frame);
	free(frame); //data no free
	
	msg_destroy(&msg); 
	return zmsg;
}


int test_frame(){
	frame_t* frame;
	struct  evbuffer* buf;
	int rc;
	char data[103224];
	int i;
	for(i=0;i<sizeof(data);i++) data[i] = 'a';
	data[sizeof(data)-1] = 0;

	buf = evbuffer_new();
	frame = frame_newstr(data);
	evbuffer_add_frame(buf, frame);   

	frame = evbuffer_read_frame(buf);
	rc = frame_streq(frame, data);
	printf("%d\n", rc); 

	evbuffer_free(buf);

	printf("==OK==\n");
	getchar();
	return 0;
}
void test_zmsg()
{
	frame_t* frame;
	msg_t* msg;
	struct  evbuffer* buf;
	int i, j;
	buf = evbuffer_new();
	for(i=0;i<100;i++){

		msg = msg_new();
		for(j=0;j<10;j++){
			char value[32];
			sprintf(value, "frame%d", j);
			msg_push_back(msg, frame_newstr(value));
		} 
		
		evbuffer_add_msg(buf, msg); 
		printf(">>>buffer len: %d\n", evbuffer_get_length(buf));
	}

	
	while(msg = evbuffer_read_msg(buf)){ 
		printf("<<<left len: %d\n", evbuffer_get_length(buf));
		while(frame = msg_pop_front(msg)){
			char* value = frame_strdup(frame);
			printf("%s\n", value);
			free(value);
			frame_destroy(&frame); 
		}
		msg_destroy(&msg);
	} 



	evbuffer_free(buf);
	printf("==OK==\n");
	getchar(); 
}

int test_convert(int argc, char* argv[]){

	msg_t* msg; 
	zmsg_t* zmsg;
	int i;

	msg = msg_new();
	msg_push_back(msg, frame_newstr("src001"));
	msg_push_back(msg, frame_newstr("dst001"));
	msg_push_back(msg, frame_newstr("msg001"));
	msg_push_back(msg, frame_newstr("mailbox"));
	msg_push_back(msg, frame_newstr("token"));

	msg_push_back(msg, frame_newstr("command"));
	msg_push_back(msg, frame_newstr("status"));
	msg_push_back(msg, frame_newstr("head=value"));
	msg_push_back(msg, frame_newstr("body"));

	for(i=0;i<1000;i++){ 
		zmsg = to_zmsg(msg); 
		//zmsg_head_set(zmsg, "key", "val");
		zmsg_print(zmsg, stdout);
		msg = to_msg(zmsg);
	}
	
	printf("==OK==\n");
	getchar();
	return 0;

}

int test_head(int argc, char *argv[])
{ 
	int i;
	zmsg_t* zmsg = zmsg_new();
	for(i=0;i<100;i++){
		char key[100], val[10];
		sprintf(key, "key%020d",i);
		sprintf(val, "val%d",i);
		zmsg_head_set(zmsg, key, val);
	} 

	zmsg_head_str(zmsg);

	for(i=0;i<100;i++){
		char key[100],*val;
		sprintf(key, "key%020d",i); 
		val = zmsg_head_get(zmsg, key);
		printf("%s\n", val);
	}

	zmsg_print(zmsg, stdout);

	printf("===ok===\n");
	getchar();
	return 0;
}