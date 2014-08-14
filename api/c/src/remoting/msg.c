#include "msg.h"   
#include "hash.h" 
 
struct meta{
	char* method;
	char* command;
	hash_t* params;

	char* status;
};


struct msg {
	struct meta* meta;
	hash_t* head;
	void*   body;
	int     body_len;
}; 


static int strempty(char* str){ 
	if(str == NULL) return 1; 
	while(isspace(*str)) str++;
	return *str == 0;
}

static char* _strndup(char* str, int n){
	char* res = malloc(n+1);
	strncpy(res, str, n);
	res[n] = '\0';
	return res;
}
static char* strndup_trim(char* str, int n){
	char* p0 = str;
	char* p1 = str+n-1;
	char* res;
	int len;
	while(*p0==' ' && p0<(str+n)) p0++;
	while(*p1==' ' && p1>str) p1--;
	len = p1-p0+1;
	if(len<1){
		return strdup("");
	}
	res = malloc(len+1);
	strncpy(res, p0, len);
	res[len] = '\0';
	return res;
}

//////////////////////////////////META LINE(First Line) HANDLE////////////////////
const char* SPLIT_CHARS = " ";
const char* const HTTP_METHODS[] = {"GET", "POST", "HEAD", "PUT", "DELETE", "OPTIONS", 0};
const hash_t* HTTP_STATUS = NULL;
const char* HTTP_STATUS_UNKNOWN = "Unknown Status";

static char* http_status(char* code){
	char* status;
	if(HTTP_STATUS == NULL){
		HTTP_STATUS = hash_new(&hash_ctrl_copy_key_val_string, NULL);
		hash_put(HTTP_STATUS, "101", "Switching Protocols");
		hash_put(HTTP_STATUS, "200", "OK");
		hash_put(HTTP_STATUS, "201", "Created");
		hash_put(HTTP_STATUS, "202", "Accepted");
		hash_put(HTTP_STATUS, "204", "No Content");
		hash_put(HTTP_STATUS, "206", "Partial Content");
		hash_put(HTTP_STATUS, "301", "Moved Permanently");
		hash_put(HTTP_STATUS, "304", "Not Modified");
		hash_put(HTTP_STATUS, "400", "Bad Request");
		hash_put(HTTP_STATUS, "401", "Unauthorized"); 
		hash_put(HTTP_STATUS, "403", "Forbidden");
		hash_put(HTTP_STATUS, "404", "Not Found");
		hash_put(HTTP_STATUS, "405", "Method Not Allowed");
		hash_put(HTTP_STATUS, "416", "Requested Range Not Satisfiable");
		hash_put(HTTP_STATUS, "500", "Internal Server Error");
	}
	status = hash_get(HTTP_STATUS, code);
	if(!status) status = HTTP_STATUS_UNKNOWN;
	return status;
}

static int is_http_method(char* m){
	int i = 0;
	if(!m) return 0;
	while(HTTP_METHODS[i]){
		if(strcmp(HTTP_METHODS[i], m) == 0) return 1;
		i++;
	}
	return 0;
}

meta_t* 
meta_new(char* meta){
	return meta_parse(meta);
}

void
meta_destroy(meta_t** self_p){
	meta_t* self;
	assert (self_p);
	self = *self_p;
	if(!self) return;

	if(self->method)
		free(self->method);
	if(self->command)
		free(self->command);
	if(self->params)
		hash_destroy(&self->params);
	if(self->status)
		free(self->status);

	free (self);
	*self_p = NULL;
}

char* meta_get_param(meta_t* self, char* key){
	assert(self);
	if(self->params == NULL) return NULL;
	return hash_get(self->params, key);
}

void meta_set_param(meta_t* self, char* key, char* val){
	assert(self);
	if(self->params == NULL){
		self->params = hash_new(&hash_ctrl_copy_key_val_string, NULL);
	}
	hash_put(self->params, key, val);
}


meta_t* meta_parse(char* meta){
	meta_t* self;
	char* method, *command, *status, *params;
	char* meta_tok, *params_tok;
	char* tok_reserved;
	char* p;

	self = (meta_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->method = strdup("GET");

	if(strempty(meta)){ 
		return self;
	}
	
	meta_tok = strdup(meta);
	method = strtok_r(meta_tok, SPLIT_CHARS, &tok_reserved);
	if(!is_http_method(method)){ 
		status = strtok_r(NULL,SPLIT_CHARS, &tok_reserved);
		if(!status){
			perror("invalid meta, missing status");
		} else {
			self->status = strdup(status);
		}
		goto DONE;
	}  
	
	free(self->method);
	self->method = strdup(method);
	
	command = strtok_r(NULL, SPLIT_CHARS,&tok_reserved);
	if(!command){
		perror("invalid meta, missing command");
		goto DONE;
	}  

	if(command[0] == '/'){
		command = command+1; //omit first '/'
	}
	p = strchr(command, '?');
	if(!p){
		self->command = strdup(command); 
		goto DONE;
	}
	
	params = p+1;
	params_tok = strdup(params);
	self->command = _strndup(command,p-command);  

	p = strtok_r(params_tok, "&", &tok_reserved);
	while(p){ 
		char* key, * val;
		val = strchr(p, '='); 
		if(val){
			key = _strndup(p, val-p);
			meta_set_param(self, key, val+1); 
			free(key);
		}
		p = strtok_r(NULL, "&", &tok_reserved);
	}
	free(params_tok);

DONE:
	free(meta_tok);
	return self;
}
 

void meta_encode(meta_t* self, iobuf_t* buf){ 
	if(self->status){ 
		iobuf_putstr(buf,"HTTP/1.1 ");
		iobuf_putstr(buf,self->status);
		iobuf_putstr(buf," ");
		iobuf_putstr(buf, http_status(self->status));
		iobuf_putstr(buf,"\r\n");
		return;
	}
	if(!self->command) return; //encode command
	
	iobuf_putstr(buf,self->method);
	iobuf_putstr(buf," /");
	iobuf_putstr(buf, self->command); 
	if(self->params && hash_size(self->params)>0){
		hash_iter_t* iter;
		hash_entry_t* e; 
		iter = hash_iter_new(self->params);
		e = hash_iter_next(iter);
		iobuf_putstr(buf, "?");
		while(e){
			iobuf_putstr(buf, hash_entry_key(e));
			iobuf_putstr(buf,  "=");
			iobuf_putstr(buf, hash_entry_val(e));
			e = hash_iter_next(iter);
			if(e){
				iobuf_putstr(buf, "&");
			}
		}
		hash_iter_destroy(&iter); 
	}  
	iobuf_putstr(buf, " HTTP/1.1\r\n"); 
}

//////////////////////////MESSAGE////////////////////
msg_t *
msg_new (void){
    msg_t* self;
    self = (msg_t *) malloc (sizeof (*self));
	memset(self, 0, sizeof(*self));
    if (self) {
		self->meta = meta_new(NULL);
		self->head = hash_new(&hash_ctrl_copy_key_val_string, NULL);
    }
    return self;
}
 

void
msg_destroy (msg_t** self_p){
	msg_t* self;
    assert (self_p);
    self = *self_p;
    if(!self) return;
    	
	if(self->meta)
		meta_destroy(&self->meta);
	if(self->head)
		hash_destroy(&self->head);
	if(self->body)
		free(self->body);

    free (self);
    *self_p = NULL;
}

void 
msg_set_meta(msg_t* self, char* meta){
	assert(self);
	if(self->meta) meta_destroy(&self->meta);
	self->meta = meta_new(meta);
}


char*
msg_get_head (msg_t *self, char* key){
	assert(self && self->head); 
	return hash_get(self->head, key);
}

void 
msg_set_head(msg_t* self, char* key, char* val){
	assert(self && self->head); 
	if(val == NULL) return;
	hash_put(self->head, key, val);
}

char*
msg_get_head_or_param(msg_t* self, char* key){
	char* val = msg_get_head(self, key);
	if(val == NULL){
		val = meta_get_param(self->meta, key);
	}
	return val;
}

void msg_set_body_nocopy(msg_t* self, void* body, int len){
	char body_len[64];
	sprintf(body_len, "%d",  len);
	self->body = body;
	self->body_len = len;
	msg_set_head(self, "content-length", body_len);
}

void msg_set_body_copy(msg_t* self, void* body, int len){
	void* new_body = malloc(len);
	memcpy(new_body,body, len);
	msg_set_body_nocopy(self, new_body, len);
}

void msg_set_body(msg_t* self, char* body){
	msg_set_body_copy(self, body, strlen(body));
}

void msg_set_bodyfmt(msg_t* self, const char* format, ...){
	char buf[1024];
	va_list argptr;
	va_start (argptr, format);
	vsprintf (buf, format, argptr);
	va_end (argptr);
	msg_set_body_copy(self, buf, strlen(buf));
}

char* msg_copy_body(msg_t* self){
	char* res = malloc(self->body_len+1);
	memcpy(res, self->body, self->body_len);
	res[self->body_len] = '\0';
	return res;
}
void* msg_get_body(msg_t* self){
	return self->body;
}
int msg_get_body_len(msg_t* self){
	return self->body_len;
}

///////////////////////////////////////////////////////////////////
char* msg_get_mq_reply(msg_t* self){
	return msg_get_head_or_param(self, HEADER_MQ_REPLY);
}
void msg_set_mq_reply(msg_t* self, char* value){
	msg_set_head(self, HEADER_MQ_REPLY, value);
}
char* msg_get_msgid(msg_t* self){
	return msg_get_head_or_param(self, HEADER_MSGID);
}
void msg_set_msgid(msg_t* self, char* value){
	msg_set_head(self, HEADER_MSGID, value);
}
char* msg_get_msgid_raw(msg_t* self){
	return msg_get_head_or_param(self, HEADER_MSGID_RAW);
}
void msg_set_msgid_raw(msg_t* self, char* value){
	msg_set_head(self, HEADER_MSGID_RAW, value);
}
bool msg_is_ack(msg_t* self){
	char* ack = msg_get_head_or_param(self, HEADER_ACK);
	if(ack == NULL) return true;
	return strcmp(ack, "1")==0;
}

void msg_set_ack(msg_t* self, bool value){
	char str[16];
	if(value){
		sprintf(str, "1");
	} else {
		sprintf(str, "0");
	}
	msg_set_head(self, HEADER_ACK, str);
}
char* msg_get_mq(msg_t* self){
	return msg_get_head_or_param(self, HEADER_MQ);
}
void msg_set_mq(msg_t* self, char* value){
	msg_set_head(self, HEADER_MQ, value);
}
char* msg_get_token(msg_t* self){
	return msg_get_head_or_param(self, HEADER_TOKEN);
}
void msg_set_token(msg_t* self, char* value){
	msg_set_head(self, HEADER_TOKEN, value);
}
char* msg_get_topic(msg_t* self){
	return msg_get_head_or_param(self, HEADER_TOPIC);
}
void msg_set_topic(msg_t* self, char* value){
	msg_set_head(self, HEADER_TOPIC, value);
}
char* msg_get_encoding(msg_t* self){
	return msg_get_head_or_param(self, HEADER_ENCODING);
}
void msg_set_encoding(msg_t* self, char* value){
	msg_set_head(self, HEADER_ENCODING, value);
}
char* msg_get_command(msg_t* self){
	return self->meta->command;
}
void msg_set_command(msg_t* self, char* value){
	meta_t* m = self->meta;
	if(m->status){
		free(m->status);
		m->status = NULL;
	}
	if(m->command)
		free(m->command);
	if(m->params)
		hash_destroy(&m->params);
	m->command = strdup(value);
}
char* msg_get_status(msg_t* self){
	return self->meta->status;
}
void msg_set_status(msg_t* self, char* value){
	meta_t* m = self->meta;
	if(m->command){
		free(m->command);
		m->command = NULL;
	}
	if(m->params){
		hash_destroy(&m->params);
		m->params = NULL;
	}

	if(m->status)
		free(m->status);
	m->status = strdup(value);
}
static int msg_is_status(msg_t* self, char* status){
	meta_t* m = self->meta;
	if(m->status == NULL) return 0;
	return strcmp(status, m->status)==0;
}

int msg_is_status200(msg_t* self){
	return msg_is_status(self, "200");
}
int msg_is_status404(msg_t* self){
	return msg_is_status(self, "404");
}
int msg_is_status500(msg_t* self){
	return msg_is_status(self, "500");
}




static int find_head_length(iobuf_t* buf){ 
	char* begin = iobuf_begin(buf);
	char* p = begin;
	char* end = iobuf_end(buf);
	while(p+3<end){
		if(*(p+0)=='\r' && *(p+1)=='\n' && *(p+2)=='\r' && *(p+3)=='\n'){
			return p+4-begin; 
		}
		p++;
	}
	return -1;
}

static msg_t* msg_parse_head(char* buf){
	msg_t* msg = msg_new();
	char* tok_reserved;
	char* p = strtok_r(buf,"\r\n", &tok_reserved);
	if(!p){
		perror("missing meta");
		return msg;
	}
	msg_set_meta(msg, p); 
	p = strtok_r(NULL,"\r\n", &tok_reserved);
	while(p){
		char* d = strchr(p, ':');
		if(d){//omit not key value
			char* key = strndup_trim(p, d-p);
			char* val = strndup_trim(d+1, p+strlen(p)-d-1);
			hash_put(msg->head, key,val); 
			free(key);
			free(val);
		}
		p = strtok_r(NULL,"\r\n", &tok_reserved);
	}
	return msg;
}


void msg_encode(msg_t* self, iobuf_t* buf){
	hash_iter_t* iter;
	hash_entry_t* e;
	assert(self);
	assert(buf);
	meta_encode(self->meta, buf);
	iter = hash_iter_new(self->head);
	e = hash_iter_next(iter);
	while(e){
		iobuf_putkv(buf, hash_entry_key(e), hash_entry_val(e));
		e = hash_iter_next(iter);
	}
	hash_iter_destroy(&iter);
	iobuf_putstr(buf, "\r\n");
	if(self->body){
		iobuf_put(buf, self->body, self->body_len);
	}
}

msg_t* msg_decode(iobuf_t* buf){ 
	msg_t* msg;
	char* head_str,* p;
	void* body;
	int head_len, body_len; 
	head_len = find_head_length(buf);
	if(head_len < 0){
		return NULL;
	} 
	iobuf_mark(buf); 
	head_str = malloc(head_len+1); 
	iobuf_get(buf, head_str, head_len);
	head_str[head_len] = '\0';
	msg = msg_parse_head(head_str);
	free(head_str); 
	
	p = msg_get_head(msg, "content-length");
	if(!p){   
		return msg;
	}

	body_len = atoi(p);
	if(iobuf_remaining(buf) < body_len){
		msg_destroy(&msg);
		iobuf_reset(buf); //������
		return NULL;
	} 
	body = malloc(body_len);
	iobuf_get(buf, body, body_len);
	msg_set_body_nocopy(msg, body, body_len); 
	return msg;
}

void msg_print(msg_t* self){
	iobuf_t* buf = iobuf_new(1024);
	msg_encode(self, buf);
	iobuf_print(buf);
	iobuf_destroy(&buf);
}


//////////////////////////IOBUF/////////////////////////

iobuf_t* iobuf_new(int capacity){ 
	void* array;
	assert(capacity>0);
	array = malloc(capacity);
	return iobuf_wrap(array, capacity);
}

iobuf_t* iobuf_dup(iobuf_t* buf){
	iobuf_t* self = malloc(sizeof(*self)); 
	memset(self, 0, sizeof(*self)); 
	self->capacity = buf->capacity;
	self->data = buf->data;
	self->position = buf->position;
	self->limit = buf->limit;
	self->mark = buf->mark;
	self->own_data = 0;
	return self;
}

iobuf_t* iobuf_wrap(char array[], int len){
	iobuf_t* self = malloc(sizeof(*self)); 
	memset(self, 0, sizeof(*self)); 
	self->capacity = len;
	self->data = array;
	self->position = 0;
	self->limit = len;
	self->mark = -1;
	self->own_data = 1;
	return self;
}

int iobuf_mv(iobuf_t* self, int n){
	if(n>self->position) return 0;
	memcpy(self->data, self->data+n, self->position-n);
	self->position -= n;
	if(self->mark > self->position) self->mark = -1;  
	return n;
}

int iobuf_auto_expand(iobuf_t* self, int need){  
	int new_cap = self->capacity;
	int new_size = self->position + need;
	char* new_data;
	if(self->own_data == 0) return -1; //can not expand for duplicated
	while(new_size>new_cap){
		new_cap *= 2;
	}
	if(new_cap == self->capacity) return 0;//nothing changed

	new_data = malloc(new_cap);
	memcpy(new_data, self->data, self->capacity);
	free(self->data);
	self->data = new_data;
	self->capacity = new_cap;
	self->limit = new_cap;
	return 1;
}
void iobuf_print(iobuf_t* self){
	char* data = malloc(self->position+1);
	memcpy(data, self->data, self->position);
	data[self->position] = '\0';
	printf("%s\n", data);
	free(data);
}
void iobuf_destroy(iobuf_t** self_p){
	iobuf_t* self = *self_p;
	if(!self) return;   
	if(self->data && self->own_data){
		free(self->data);
	}
	free(self);
	*self_p = NULL;
}

void iobuf_mark(iobuf_t* self){
	self->mark = self->position;
}
void iobuf_reset(iobuf_t* self){
	int m = self->mark;
	if(m < 0){
		perror("mark not set, reset discard");
		return;
	}
	self->position = m;
}
int iobuf_remaining(iobuf_t* self){
	return self->limit - self->position;
}
iobuf_t* iobuf_flip(iobuf_t* self){
	self->limit = self->position;
	self->position = 0;
	self->mark = -1;
	return self;
}
char* iobuf_begin(iobuf_t* self){
	assert(self);
	return self->data+self->position;
}
char* iobuf_end(iobuf_t* self){
	assert(self);
	return self->data+self->limit;
}

iobuf_t* iobuf_limit(iobuf_t* self, int new_limit){
	if(new_limit>self->capacity || new_limit<0){
		perror("set new limit error, discarding");
		return self;
	}
	self->limit = new_limit;
	if(self->position > self->limit) self->position = self->limit;
	if(self->mark > self->limit) self->mark = -1; 
	return self;
}


int iobuf_get(iobuf_t* self, char data[], int len){ 
	int copy_len = iobuf_copyout(self, data, len);
	if(copy_len>0){
		iobuf_drain(self, len); 
	}
	return copy_len;
}

int iobuf_copyout(iobuf_t* self, char data[], int len){ 
	if(iobuf_remaining(self)<len){
		return -1;
	}
	memcpy(data, self->data+self->position, len); 
	return len;
}

int iobuf_put(iobuf_t* self, void* data, int len){
	iobuf_auto_expand(self, len);
	memcpy(self->data+self->position, data, len);
	iobuf_drain(self, len); 
	return len;
}

int iobuf_putbuf(iobuf_t* self, iobuf_t* buf){
	return iobuf_put(self, buf->data+buf->position, iobuf_remaining(buf));
}
int iobuf_putstr(iobuf_t* self, char* str){
	return iobuf_put(self, str, strlen(str));
}

int iobuf_putkv(iobuf_t* self, char* key, char* val){
	int len = 0;
	len += iobuf_putstr(self, key);
	len += iobuf_putstr(self, ": ");
	len += iobuf_putstr(self, val);
	len += iobuf_putstr(self, "\r\n");
	return len;
}

int iobuf_drain(iobuf_t* self, int n){
	int res = n;
	int new_pos;
	if(n<=0) return 0;
	new_pos = self->position +n;
	if(new_pos>self->limit){
		new_pos = self->limit;
		res = new_pos - self->position;
	}
	self->position = new_pos; 
	if(self->mark > self->position) self->mark = -1;  
	return res;
}
 

int test_msg(int argc, char* argv[]){
	int i;
	iobuf_t* buf = iobuf_new(1);
	msg_t* msg = msg_new();
	msg_set_command(msg, "produce");
	msg_set_mq(msg, "MyMQ");
	for(i=0;i<1000000;i++){
		msg_encode(msg, buf);
		iobuf_print(buf);
		iobuf_flip(buf);
		msg_destroy(&msg);
		msg = msg_decode(buf); 
	}
	printf("=done=");
	getchar();
	return 0;
}


 