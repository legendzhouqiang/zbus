#include "msg.h" 
#include "hash.h" 
#include "buffer.h"


struct meta{
	char* status;

	char* method;
	char* uri;
	
	char* path;
	hash_t* params;
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
	char* res = (char*)malloc(n+1);
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
	res = (char*)malloc(len+1);
	strncpy(res, p0, len);
	res[len] = '\0';
	return res;
}

//////////////////////////////////META LINE(First Line) HANDLE////////////////////
meta_t* meta_new(char* meta);
void    meta_destroy(meta_t** self_p);
char*   meta_get_param(meta_t* self, char* key);
void    meta_set_param(meta_t* self, char* key, char* val);
meta_t* meta_parse(char* meta);
void    meta_encode(meta_t* self, buf_t* buf);

const char* SPLIT_CHARS = " ";
const char* const HTTP_METHODS[] = {"GET", "POST", "HEAD", "PUT", "DELETE", "OPTIONS", 0};
hash_t* HTTP_STATUS = NULL;
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
	status = (char*)hash_get(HTTP_STATUS, code);
	if(!status) status = (char*)HTTP_STATUS_UNKNOWN;
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
	if(self->uri)
		free(self->uri);

	if(self->path)
		free(self->path);

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
	return (char*)hash_get(self->params, key);
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
	char* method, *uri, *status, *params;
	char* meta_tok, *params_tok;
	char* tok_reserved;
	char* p;

	self = (meta_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));
	self->method = strdup("GET");
	self->uri = strdup("/");

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
	
	uri = strtok_r(NULL, SPLIT_CHARS,&tok_reserved);
	if(!uri){
		perror("invalid meta, missing command");
		goto DONE;
	}  

	if(uri[0] == '/'){
		uri = uri+1; //omit first '/'
	}
	p = strchr(uri, '?');
	if(!p){
		self->uri = strdup(uri); 
		goto DONE;
	}
	
	params = p+1;
	params_tok = strdup(params);
	self->uri = _strndup(uri,p-uri);  

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
 

void meta_encode(meta_t* self, buf_t* buf){ 
	if(self->status){ 
		buf_putstr(buf,"HTTP/1.1 ");
		buf_putstr(buf,self->status);
		buf_putstr(buf," ");
		buf_putstr(buf, http_status(self->status));
		buf_putstr(buf,"\r\n");
		return;
	}
	char* url = "/";
	if(!self->uri){
		url = self->uri;
	}
	buf_putstr(buf,self->method);
	buf_putstr(buf," ");
	buf_putstr(buf, url); 
	buf_putstr(buf, " HTTP/1.1\r\n"); 
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
	return (char*)hash_get(self->head, key);
}

void
msg_remove_head (msg_t *self, char* key){
	assert(self && self->head); 
	hash_rem(self->head, key);
}

void 
msg_set_head(msg_t* self, char* key, char* val){
	assert(self && self->head); 
	if(val == NULL) return;
	hash_put(self->head, key, val);
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
void msg_set_json_body(msg_t* self, char* body){
	msg_set_head(self, HEADER_CONTENT_TYPE, "application/json");
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
	char* res = (char*)malloc(self->body_len+1);
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
char* msg_get_uri(msg_t* self){
	return self->meta->uri;
}
char* msg_get_path(msg_t* self){
	return self->meta->path;
}

///////////////////////////////////////////////////////////////////
char* msg_get_sender(msg_t* self){
	return msg_get_head(self, HEADER_SENDER);
}
void msg_set_sender(msg_t* self, char* value){
	msg_set_head(self, HEADER_SENDER, value);
}
char* msg_get_recver(msg_t* self){
	return msg_get_head(self, HEADER_RECVER);
}
void msg_set_recver(msg_t* self, char* value){
	msg_set_head(self, HEADER_RECVER, value);
}
char* msg_get_id(msg_t* self){
	return msg_get_head(self, HEADER_ID);
}
void msg_set_id(msg_t* self, char* value){
	msg_set_head(self, HEADER_ID, value);
}
char* msg_get_rawid(msg_t* self){
	return msg_get_head(self, HEADER_ORIGIN_ID);
}
void msg_set_rawid(msg_t* self, char* value){
	msg_set_head(self, HEADER_ORIGIN_ID, value);
}
bool msg_is_ack(msg_t* self){
	char* ack = msg_get_head(self, HEADER_ACK);
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
char* msg_get_topic(msg_t* self){
	return msg_get_head(self, HEADER_TOPIC);
}
void msg_set_topic(msg_t* self, char* value){
	msg_set_head(self, HEADER_TOPIC, value);
} 
char* msg_get_encoding(msg_t* self){
	return msg_get_head(self, HEADER_ENCODING);
}
void msg_set_encoding(msg_t* self, char* value){
	msg_set_head(self, HEADER_ENCODING, value);
}
char* msg_get_reply_code(msg_t* self){
	return msg_get_head(self, HEADER_REPLY_CODE);
}
void msg_set_reply_code(msg_t* self, char* value){
	msg_set_head(self, HEADER_REPLY_CODE, value);
}


char* msg_get_cmd(msg_t* self){
	return msg_get_head(self, HEADER_CMD);
}
void msg_set_cmd(msg_t* self, char* value){
	msg_set_head(self, HEADER_CMD, value);
} 

char* msg_get_status(msg_t* self){
	return self->meta->status;
}
void msg_set_status(msg_t* self, char* value){
	meta_t* m = self->meta;
	if(m->status)
		free(m->status);
	if(value == NULL){
		m->status = NULL;
	} else {
		m->status = strdup(value);
	}
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




static int find_head_length(buf_t* buf){ 
	char* begin = buf_begin(buf);
	char* p = begin;
	char* end = buf_end(buf);
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


void msg_encode(msg_t* self, buf_t* buf){
	hash_iter_t* iter;
	hash_entry_t* e;
	assert(self);
	assert(buf);
	meta_encode(self->meta, buf);
	iter = hash_iter_new(self->head);
	e = hash_iter_next(iter);
	while(e){
		buf_putkv(buf, (char*)hash_entry_key(e), (char*)hash_entry_val(e));
		e = hash_iter_next(iter);
	}
	hash_iter_destroy(&iter);
	buf_putstr(buf, "\r\n");
	if(self->body){
		buf_put(buf, self->body, self->body_len);
	}
}

msg_t* msg_decode(buf_t* buf){ 
	msg_t* msg;
	char* head_str,* p;
	void* body;
	int head_len, body_len; 
	head_len = find_head_length(buf);
	if(head_len < 0){
		return NULL;
	} 
	buf_mark(buf); 
	head_str = (char*)malloc(head_len+1); 
	buf_get(buf, head_str, head_len);
	head_str[head_len] = '\0';
	msg = msg_parse_head(head_str);
	free(head_str); 
	
	p = msg_get_head(msg, "content-length");
	if(!p){   
		return msg;
	}

	body_len = atoi(p);
	if(buf_remaining(buf) < body_len){
		msg_destroy(&msg);
		buf_reset(buf); 
		return NULL;
	} 
	body = malloc(body_len);
	buf_get(buf, (char*)body, body_len);
	msg_set_body_nocopy(msg, body, body_len); 
	return msg;
}

void msg_print(msg_t* self){
	buf_t* buf = buf_new(1024);
	msg_encode(self, buf);
	buf_print(buf);
	buf_destroy(&buf);
}


 