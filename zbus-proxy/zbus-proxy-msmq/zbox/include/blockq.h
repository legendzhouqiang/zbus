#ifndef _ZBOX_MQ_H_
#define _ZBOX_MQ_H_

#include "thread.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef struct blockq blockq_t;

blockq_t*
	blockq_new();
void 
	blockq_push(blockq_t *self, void *msg);

void* 
	blockq_pop(blockq_t *self);

int blockq_length(blockq_t *self);
void blockq_destroy(blockq_t **self_p);


#ifdef __cplusplus
}
#endif

#endif