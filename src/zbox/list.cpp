#include "include/prelude.h"
#include "include/list.h" 


list_t *
list_new(void){
    struct _list_t *list;

    if ((list = (list_t*)zmalloc(sizeof(*list))) == NULL)
        return NULL;
    list->head = list->tail = NULL;
    list->len = 0;
    list->dup = NULL;
    list->destroy = NULL;
    list->match = NULL;
    return list;
}


void
list_destroy(list_t** self_p){
	if(!self_p) return;
	list_t* self = *self_p;
	if(self){
		unsigned int len;
		list_node_t *current, *next;

		current = self->head;
		len = self->len;
		while(len--) {
			next = current->next;
			if (self->destroy) self->destroy(&current->value);
			zfree(current);
			current = next;
		}
		zfree(self);
		*self_p = NULL;
	}
}


list_t *
list_push_front(list_t *list, void *value){
    list_node_t *node;

    if ((node = (list_node_t*)zmalloc(sizeof(*node))) == NULL)
        return NULL;
    node->value = value;
    if (list->len == 0) {
        list->head = list->tail = node;
        node->prev = node->next = NULL;
    } else {
        node->prev = NULL;
        node->next = list->head;
        list->head->prev = node;
        list->head = node;
    }
    list->len++;
    return list;
}


list_t *
list_push_back(list_t *list, void *value){
    list_node_t *node;

    if ((node = (list_node_t *)zmalloc(sizeof(*node))) == NULL)
        return NULL;
    node->value = value;
    if (list->len == 0) {
        list->head = list->tail = node;
        node->prev = node->next = NULL;
    } else {
        node->prev = list->tail;
        node->next = NULL;
        list->tail->next = node;
        list->tail = node;
    }
    list->len++;
    return list;
}


list_t *
list_insert(list_t *list, list_node_t *old_node, void *value, int after) {
    list_node_t *node;

    if ((node = (list_node_t *)zmalloc(sizeof(*node))) == NULL)
        return NULL;
    node->value = value;
    if (after) {
        node->prev = old_node;
        node->next = old_node->next;
        if (list->tail == old_node) {
            list->tail = node;
        }
    } else {
        node->next = old_node;
        node->prev = old_node->prev;
        if (list->head == old_node) {
            list->head = node;
        }
    }
    if (node->prev != NULL) {
        node->prev->next = node;
    }
    if (node->next != NULL) {
        node->next->prev = node;
    }
    list->len++;
    return list;
}

void
list_remove_node(list_t *list, list_node_t *node){
    if (node->prev)
        node->prev->next = node->next;
    else
        list->head = node->next;
    if (node->next)
        node->next->prev = node->prev;
    else
        list->tail = node->prev;
    if (list->destroy) list->destroy(&node->value);
    zfree(node);
    list->len--;
}

void
list_remove(list_t *list, void *value){
	list_node_t* node = list_find(list, value);
	if(node){
		list_remove_node(list, node);
	}
}


void *
list_pop_front(list_t *list){
	if(!list->head) return NULL;
	list_node_t* node = list->head;
	list->head = node->next;
	if(list->head == NULL){
		list->tail = NULL;
	}else{
		list->head->prev = NULL;
	}
	list->len--;
	void* value = node->value;
	zfree(node);
	return value;
}

void *
list_pop_back(list_t *list){
	if(!list->tail) return NULL;
	list_node_t* node = list->tail;
	list->tail = node->prev;
	if(list->tail == NULL){
		list->head = NULL;
	}else{
		list->tail->next = NULL;
	}
	list->len--;
	void* value = node->value;
	zfree(node);
	return value;
}


list_iter_t *
list_iter_new(list_t *list, int direction){
    list_iter_t *iter;
    
    if ((iter = (list_iter_t *)zmalloc(sizeof(*iter))) == NULL) return NULL;
    if (direction == LIST_ITER_FORWARD)
        iter->next = list->head;
    else
        iter->next = list->tail;
    iter->direction = direction;
    return iter;
}


void
list_iter_destroy(list_iter_t ** self_p) {
	if(!self_p) return;
	list_iter_t* iter = *self_p;
	if(iter){
		zfree(iter);
		*self_p = NULL;
	}
}


void
list_iter_reset(list_t *list, list_iter_t *li) {
    li->next = list->head;
    li->direction = LIST_ITER_FORWARD;
}

void
list_iter_reset_tail(list_t *list, list_iter_t *li) {
    li->next = list->tail;
    li->direction = LIST_ITER_BACKWARD;
}


list_node_t *
list_iter_next_node(list_iter_t *iter){
    list_node_t *current = iter->next;

    if (current != NULL) {
        if (iter->direction == LIST_ITER_FORWARD)
            iter->next = current->next;
        else
            iter->next = current->prev;
    }
    return current;
}

list_node_t *
list_iter_next(list_iter_t *iter){
    list_node_t *node = list_iter_next_node(iter);
    return node? (list_node_t*)list_value(node) : NULL;
}

list_t *
list_dup(list_t *orig){
    list_t *copy;
    list_iter_t *iter;
    list_node_t *node;

    if ((copy = list_new()) == NULL)
        return NULL;
    copy->dup = orig->dup;
    copy->destroy = orig->destroy;
    copy->match = orig->match;
    iter = list_iter_new(orig, LIST_ITER_FORWARD);
    while((node = list_iter_next_node(iter)) != NULL) {
        void *value;

        if (copy->dup) {
            value = copy->dup(node->value);
            if (value == NULL) {
                list_destroy(&copy);
                list_iter_destroy(&iter);
                return NULL;
            }
        } else
            value = node->value;
        if (list_push_back(copy, value) == NULL) {
            list_destroy(&copy);
            list_iter_destroy(&iter);
            return NULL;
        }
    }
    list_iter_destroy(&iter);
    return copy;
}


list_node_t *
list_find(list_t *list, void *value){
    list_iter_t *iter;
    list_node_t *node;

    iter = list_iter_new(list, LIST_ITER_FORWARD);
    while((node = list_iter_next_node(iter)) != NULL) {
        if (list->match) {
            if (list->match(node->value, value)) {
                list_iter_destroy(&iter);
                return node;
            }
        } else {
            if (value == node->value) {
                list_iter_destroy(&iter);
                return node;
            }
        }
    }
    list_iter_destroy(&iter);
    return NULL;
}


list_node_t *
list_at(list_t *list, int index) {
    list_node_t *n;

    if (index < 0) {
        index = (-index)-1;
        n = list->tail;
        while(index-- && n) n = n->prev;
    } else {
        n = list->head;
        while(index-- && n) n = n->next;
    }
    return n;
}

 
