#ifndef __ZBOX_HASH_H__
#define __ZBOX_HASH_H__

#ifdef __cplusplus
extern "C" {
#endif

#define HASH_OK 0
#define HASH_ERR 1

/* Unused arguments generate annoying warnings... */
#define HASH_NOTUSED(V) ((void) V)

typedef struct _hash_entry_t {
    void *key;
    void *val;
    struct _hash_entry_t *next;
} hash_entry_t;

typedef struct _hash_ctrl_t {
    unsigned int (*hash_func)(const void *key);  //hash function
    void *(*key_dup)(void *privdata, const void *key); //key duplication
    void *(*val_dup)(void *privdata, const void *obj); //value duplication
    int (*key_cmp)(void *privdata, const void *key1, const void *key2); //key compare
    void (*key_destroy)(void *privdata, void *key); //key destroy
    void (*val_destroy)(void *privdata, void *obj); //value destroy
} hash_ctrl_t;

/* This is our hash table structure. Every dictionary has two of this as we
 * implement incremental rehashing, for the old to the new table. */
typedef struct _hash_table_t {
    hash_entry_t **table;
    unsigned long size;
    unsigned long sizemask;
    unsigned long used;
} hash_table_t;

typedef struct _hash_t {
    hash_ctrl_t *type;
    void *privdata;
    hash_table_t ht[2];
    int rehashidx; /* rehashing not in progress if rehashidx == -1 */
    int iterators; /* number of iterators currently running */
} hash_t;

/* If safe is set to 1 this is a safe iteartor, that means, you can call
 * dictAdd, dictFind, and other functions against the dictionary even while
 * iterating. Otherwise it is a non safe iterator, and only dictNext()
 * should be called while iterating. */
typedef struct _hash_iter_t {
    hash_t *d;
    int table, index, safe;
    hash_entry_t *entry, *next_entry;
} hash_iter_t;

/* This is the initial size of every hash table */
#define HASH_HT_INITIAL_SIZE     4

/* ------------------------------- Macros ------------------------------------*/
#define hash_entry_free_key(d, entry) \
    if ((d)->type->key_destroy) \
        (d)->type->key_destroy((d)->privdata, (entry)->key)

#define hash_entry_free_val(d, entry) \
    if ((d)->type->val_destroy) \
        (d)->type->val_destroy((d)->privdata, (entry)->val)


#define hash_entry_set_key(d, entry, _key_) do { \
    if ((d)->type->key_dup) \
        entry->key = (d)->type->key_dup((d)->privdata, _key_); \
    else \
        entry->key = (_key_); \
} while(0)

#define hash_entry_set_val(d, entry, _val_) do { \
    if ((d)->type->val_dup) \
        entry->val = (d)->type->val_dup((d)->privdata, _val_); \
    else \
        entry->val = (_val_); \
} while(0)



#define hash_key_compare(d, key1, key2) \
    (((d)->type->key_cmp) ? \
        (d)->type->key_cmp((d)->privdata, key1, key2) : \
        (key1) == (key2))


#define hash_key(d, key) (d)->type->hash_func(key)
#define hash_entry_key(he) ((he)->key)
#define hash_entry_val(he) ((he)->val)

#define hash_slots(d) ((d)->ht[0].size+(d)->ht[1].size)
#define hash_size(d) ((d)->ht[0].used+(d)->ht[1].used)
#define hash_is_rehashing(ht) ((ht)->rehashidx != -1)

/* API */
ZBOX_EXPORT hash_t *
	hash_new(hash_ctrl_t *type, void *privDataPtr);
ZBOX_EXPORT void
	hash_destroy(hash_t** self_p);
ZBOX_EXPORT int
	hash_expand(hash_t *d, unsigned long size);
ZBOX_EXPORT int
	hash_resize(hash_t *d);
ZBOX_EXPORT void
	hash_clear(hash_t *d);
ZBOX_EXPORT void
	hash_enable_resize(void);
ZBOX_EXPORT void
	hash_disable_resize(void);
ZBOX_EXPORT int
	hash_rehash(hash_t *d, int n);
ZBOX_EXPORT int
	hash_rehash_millis(hash_t *d, int ms);

//
ZBOX_EXPORT int
	hash_add(hash_t *d, void *key, void *val);
ZBOX_EXPORT int
	hash_put(hash_t *d, void *key, void *val);
ZBOX_EXPORT int
	hash_rem(hash_t *d, const void *key);
ZBOX_EXPORT int
	hash_rem_nofree(hash_t *d, const void *key);
ZBOX_EXPORT void *
	hash_get(hash_t *d, const void *key);
ZBOX_EXPORT hash_entry_t *
	hash_entry(hash_t *d, const void *key);
ZBOX_EXPORT hash_entry_t *
	hash_random(hash_t *d);


ZBOX_EXPORT hash_iter_t *
	hash_iter_new(hash_t *d);
ZBOX_EXPORT hash_iter_t *
	hash_iter_new_safe(hash_t *d);
ZBOX_EXPORT hash_entry_t *
	hash_iter_next(hash_iter_t *iter);
ZBOX_EXPORT void
	hash_iter_destroy(hash_iter_t **iter_p);


ZBOX_EXPORT void
	hash_stats(hash_t *d);

ZBOX_EXPORT unsigned int
	hash_func_gen(const unsigned char *buf, int len);
ZBOX_EXPORT unsigned int
	hash_func_gen_nocase(const unsigned char *buf, int len);
ZBOX_EXPORT unsigned int
	hash_func_string(const void *key);
ZBOX_EXPORT void *
	hash_dup_string(void *privdata, const void *key);
ZBOX_EXPORT int
	hash_cmp_string(void *privdata, const void *key1, const void *key2);
ZBOX_EXPORT void
	hash_destroy_string(void *privdata, void *key); 
#ifdef __cplusplus
}
#endif

#endif /* __DICT_H */
