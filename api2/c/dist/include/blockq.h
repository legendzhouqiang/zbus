#ifndef _ZBOX_MQ_H_
#define _ZBOX_MQ_H_

#include "platform.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef struct blockq blockq_t;

ZBOX_EXPORT blockq_t*
	blockq_new();
ZBOX_EXPORT void 
	blockq_destroy(blockq_t **self_p);

ZBOX_EXPORT void 
	blockq_push(blockq_t *self, void *msg);
ZBOX_EXPORT void* 
	blockq_pop(blockq_t *self);
ZBOX_EXPORT void* 
	blockq_pop_timed(blockq_t *self, int64_t millis_timeout);
ZBOX_EXPORT int 
	blockq_length(blockq_t *self);



#ifdef __cplusplus
}
#endif

#endif