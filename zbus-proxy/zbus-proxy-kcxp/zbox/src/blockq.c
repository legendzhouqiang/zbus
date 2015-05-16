#include "thread.h"
#include "blockq.h"


typedef struct node {
	void* msg;
	struct node *next;
} node_t;


struct blockq {
	int length;
	pthread_mutex_t mutex;
	pthread_cond_t cond;
	node_t *first, *last; 
};

blockq_t * blockq_new(){
	blockq_t* self = (blockq_t*)malloc(sizeof(*self));
	memset(self, 0, sizeof(*self));

	int rc = pthread_cond_init(&self->cond, NULL);
	if(rc != 0){
		free(self);
		return NULL;
	}
	rc = pthread_mutex_init(&self->mutex, NULL);
	if (rc != 0) {
		pthread_cond_destroy(&self->cond);
		free(self);
		return NULL;
	}
	return self;
}

void blockq_push(blockq_t *self, void *data){
	node_t * node = (node_t *)malloc(sizeof(*node));
	memset(node, 0, sizeof(*node));

	pthread_mutex_lock(&self->mutex);
	node->msg = data;
	node->next = NULL;

	if (self->last == NULL) {
		self->last = node;
		self->first = node;
	} else {
		self->last->next = node;
		self->last = node;
	}

	if(self->length == 0)
		pthread_cond_broadcast(&self->cond);
	self->length++;
	pthread_mutex_unlock(&self->mutex);
}


void* blockq_pop(blockq_t *self) {
	assert(self); 
	
	void* res;
	pthread_mutex_lock(&self->mutex);

	while (self->first == NULL) {  //Need to loop to handle spurious wakeups
		pthread_cond_wait(&self->cond, &self->mutex);
	}

	node_t* node = self->first;
	self->first = self->first->next;
	self->length--;

	if (self->first == NULL) {
		self->last = NULL;     // we know this since we hold the lock
		self->length = 0;
	}
	res = node->msg; 
	free(node);

	pthread_mutex_unlock(&self->mutex);
	return res;
}


void blockq_destroy(blockq_t **self_p){
	blockq_t* self = *self_p;
	if(self == NULL) return;

	pthread_mutex_lock(&self->mutex);
	node_t* cur = self->first;

	while(cur){
		node_t* node = cur;
		free(node);
		cur = cur->next;
	}
	pthread_mutex_unlock(&self->mutex);

	pthread_mutex_destroy(&self->mutex);
	pthread_cond_destroy(&self->cond);
}

int blockq_length(blockq_t *self){
	long counter;
	pthread_mutex_lock(&self->mutex);
	counter = self->length;
	pthread_mutex_unlock(&self->mutex);

	return counter;
}