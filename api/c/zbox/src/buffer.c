#include "buffer.h"    

buf_t* buf_new(int capacity){ 
	void* array;
	assert(capacity>0);
	array = malloc(capacity);
	return buf_wrap((char*)array, capacity);
}

buf_t* buf_dup(buf_t* buf){
	buf_t* self = (buf_t*)malloc(sizeof(*self)); 
	memset(self, 0, sizeof(*self)); 
	self->capacity = buf->capacity;
	self->data = buf->data;
	self->position = buf->position;
	self->limit = buf->limit;
	self->mark = buf->mark;
	self->own_data = 0;
	return self;
}

buf_t* buf_wrap(char array[], int len){
	buf_t* self = (buf_t*)malloc(sizeof(*self)); 
	memset(self, 0, sizeof(*self)); 
	self->capacity = len;
	self->data = array;
	self->position = 0;
	self->limit = len;
	self->mark = -1;
	self->own_data = 1;
	return self;
}

int buf_mv(buf_t* self, int n){
	if(n>self->position) return 0;
	memcpy(self->data, self->data+n, self->position-n);
	self->position -= n;
	if(self->mark > self->position) self->mark = -1;  
	return n;
}

int iobuf_auto_expand(buf_t* self, int need){  
	int new_cap = self->capacity;
	int new_size = self->position + need;
	char* new_data;
	if(self->own_data == 0) return -1; //can not expand for duplicated
	while(new_size>new_cap){
		new_cap *= 2;
	}
	if(new_cap == self->capacity) return 0;//nothing changed

	new_data = (char*)malloc(new_cap);
	memcpy(new_data, self->data, self->capacity);
	free(self->data);
	self->data = new_data;
	self->capacity = new_cap;
	self->limit = new_cap;
	return 1;
}
void buf_print(buf_t* self){
	char* data = (char*)malloc(self->position+1);
	memcpy(data, self->data, self->position);
	data[self->position] = '\0';
	printf("%s\n", data);
	free(data);
}
void buf_destroy(buf_t** self_p){
	buf_t* self = *self_p;
	if(!self) return;   
	if(self->data && self->own_data){
		free(self->data);
	}
	free(self);
	*self_p = NULL;
}

void buf_mark(buf_t* self){
	self->mark = self->position;
}
void buf_reset(buf_t* self){
	int m = self->mark;
	if(m < 0){
		perror("mark not set, reset discard");
		return;
	}
	self->position = m;
}
int buf_remaining(buf_t* self){
	return self->limit - self->position;
}
buf_t* buf_flip(buf_t* self){
	self->limit = self->position;
	self->position = 0;
	self->mark = -1;
	return self;
}
char* buf_begin(buf_t* self){
	assert(self);
	return self->data+self->position;
}
char* buf_end(buf_t* self){
	assert(self);
	return self->data+self->limit;
}

buf_t* buf_limit(buf_t* self, int new_limit){
	if(new_limit>self->capacity || new_limit<0){
		perror("set new limit error, discarding");
		return self;
	}
	self->limit = new_limit;
	if(self->position > self->limit) self->position = self->limit;
	if(self->mark > self->limit) self->mark = -1; 
	return self;
}


int buf_get(buf_t* self, char data[], int len){ 
	int copy_len = buf_copyout(self, data, len);
	if(copy_len>0){
		buf_drain(self, len); 
	}
	return copy_len;
}

int buf_copyout(buf_t* self, char data[], int len){ 
	if(buf_remaining(self)<len){
		return -1;
	}
	memcpy(data, self->data+self->position, len); 
	return len;
}

int buf_put(buf_t* self, void* data, int len){
	iobuf_auto_expand(self, len);
	memcpy(self->data+self->position, data, len);
	buf_drain(self, len); 
	return len;
}

int buf_putbuf(buf_t* self, buf_t* buf){
	return buf_put(self, buf->data+buf->position, buf_remaining(buf));
}
int buf_putstr(buf_t* self, char* str){
	return buf_put(self, str, strlen(str));
}

int buf_putkv(buf_t* self, char* key, char* val){
	int len = 0;
	len += buf_putstr(self, key);
	len += buf_putstr(self, ": ");
	len += buf_putstr(self, val);
	len += buf_putstr(self, "\r\n");
	return len;
}

int buf_drain(buf_t* self, int n){
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

 