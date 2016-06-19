#ifndef __ZBOX_BUFFER_H_
#define __ZBOX_BUFFER_H_

#include "platform.h"  
#ifdef __cplusplus
extern "C" {
#endif

typedef struct buf buf_t; 

struct buf{
	int mark;
	int position;
	int limit;
	int capacity; 
	int own_data;
	char* data; 
};

//IO»º´æ²Ù×÷
ZBOX_EXPORT buf_t* buf_new(int capacity);
ZBOX_EXPORT buf_t* buf_dup(buf_t* buf);
ZBOX_EXPORT buf_t* buf_wrap(char array[], int len); 
ZBOX_EXPORT void   buf_destroy(buf_t** self_p); 

ZBOX_EXPORT void buf_mark(buf_t* self);
ZBOX_EXPORT void buf_reset(buf_t* self);
ZBOX_EXPORT int  buf_remaining(buf_t* self);
ZBOX_EXPORT int  buf_drain(buf_t* self, int n);
ZBOX_EXPORT int  buf_copyout(buf_t* self, char data[], int len);
ZBOX_EXPORT int  buf_get(buf_t* self, char data[],int len);
ZBOX_EXPORT int  buf_put(buf_t* self, void* data, int len);
ZBOX_EXPORT int  buf_putstr(buf_t* self, char* str);
ZBOX_EXPORT int  buf_putkv(buf_t* self, char* key, char* val);
ZBOX_EXPORT int  buf_putbuf(buf_t* self, buf_t* buf); 

ZBOX_EXPORT char*  buf_begin(buf_t* self);
ZBOX_EXPORT char*  buf_end(buf_t* self);
ZBOX_EXPORT buf_t* buf_flip(buf_t* self);
ZBOX_EXPORT int    buf_mv(buf_t* self, int n);
ZBOX_EXPORT buf_t* buf_limit(buf_t* self, int new_limit);
ZBOX_EXPORT void   buf_print(buf_t* self);
 

#ifdef __cplusplus
}
#endif

#endif
