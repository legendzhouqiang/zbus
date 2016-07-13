#ifndef __ZBOX_LIST_H__
#define __ZBOX_LIST_H__ 

#include "platform.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef struct _list_node_t {
    struct _list_node_t *prev;
    struct _list_node_t *next;
    void *value;
} list_node_t;


typedef struct _list_t {
    list_node_t *head;
    list_node_t *tail;
    void *(*dup)(void *ptr);
    void (*destroy)(void **ptr);
    int (*match)(void *ptr, void *key);
    unsigned int len;
} list_t;

/* Functions implemented as macros */
#define list_size(l) ((l)->len)
#define list_head(l) ((l)->head)
#define list_tail(l) ((l)->tail)
#define list_prev(n) ((n)->prev)
#define list_next(n) ((n)->next)
#define list_value(n) ((n)->value)

#define list_set_dup(l,m) ((l)->dup = (m))
#define list_set_destroy(l,m) ((l)->destroy = (m))
#define list_set_match(l,m) ((l)->match = (m))

#define list_get_dup(l) ((l)->dup)
#define list_get_destroy(l) ((l)->destroy)
#define list_get_match(l) ((l)->match)


ZBOX_EXPORT list_t *
	list_new(void);
ZBOX_EXPORT list_t *
	list_dup(list_t *orig);
ZBOX_EXPORT void
	list_destroy(list_t** self_p);

ZBOX_EXPORT list_node_t *
	list_push_front(list_t *list, void *value);
ZBOX_EXPORT list_node_t *
	list_push_back(list_t *list, void *value);
ZBOX_EXPORT void *
	list_pop_front(list_t *list);
ZBOX_EXPORT void *
	list_pop_back(list_t *list);



ZBOX_EXPORT list_t *
	list_insert(list_t *list, list_node_t *old_node, void *value, bool after);
ZBOX_EXPORT void
	list_remove_node(list_t *list, list_node_t *node);
ZBOX_EXPORT void
	list_remove(list_t *list, void *value);

ZBOX_EXPORT list_node_t *
	list_find(list_t *list, void *value);
ZBOX_EXPORT list_node_t *
	list_at(list_t *list, int index);


void
	list_test();

#ifdef __cplusplus
}
#endif

#endif /* __ZLIST_H__ */
