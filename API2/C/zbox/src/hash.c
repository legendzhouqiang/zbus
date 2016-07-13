#include "platform.h"
#include "hash.h"

//fix for random on Windows
#if (defined (_WIN32) || defined (WIN32))

#include <Windows.h>
#ifndef __RTL_GENRANDOM
#define __RTL_GENRANDOM 1
typedef BOOLEAN (_stdcall* RtlGenRandomFunc)(void * RandomBuffer, ULONG RandomBufferLength);
#endif
RtlGenRandomFunc RtlGenRandom;

#define random() (long)my_replace_random()
#define rand() my_replace_random()
int my_replace_random() {
	unsigned int x=0;
	if (RtlGenRandom == NULL) {
		// load proc if not loaded
		HMODULE lib = LoadLibraryA("advapi32.dll");
		RtlGenRandom = (RtlGenRandomFunc)GetProcAddress(lib, "SystemFunction036");
		if (RtlGenRandom == NULL) return 1;
	}
	RtlGenRandom(&x, sizeof(UINT_MAX));
	return (int)(x >> 1);
}

#endif



/* ----------------------- StringCopy Hash Table Type ------------------------*/
unsigned int
hash_func_string(const void *key){
    return hash_func_gen((const unsigned char*)key, strlen((const char*)key));
}

void *
hash_dup_string(void *privdata, const void *key){
    int len = strlen((const char*)key);
    char *copy = (char*)malloc(len+1);
    HASH_NOTUSED(privdata);

    memcpy(copy, key, len);
    copy[len] = '\0';
    return copy;
}

int
hash_cmp_string(void *privdata, const void *key1,const void *key2){
    HASH_NOTUSED(privdata);
    return strcmp((const char*)key1, (const char*)key2) == 0;
}

void
hash_destroy_string(void *privdata, void *key){
    HASH_NOTUSED(privdata);
    free(key);
}


/* Using dictEnableResize() / dictDisableResize() we make possible to
 * enable/disable resizing of the hash table as needed. This is very important
 * for Redis, as we use copy-on-write and don't want to move too much memory
 * around when there is a child performing saving operations.
 *
 * Note that even when dict_can_resize is set to 0, not all resizes are
 * prevented: an hash table is still allowed to grow if the ratio between
 * the number of elements and the buckets > dict_force_resize_ratio. */
static int dict_can_resize = 1;
static unsigned int dict_force_resize_ratio = 5;

/* -------------------------- private prototypes ---------------------------- */

static int _hash_expand_if_needed(hash_t *ht);
static unsigned long _hash_next_power(unsigned long size);
static int _hash_key_index(hash_t *ht, const void *key);
static int _hash_init(hash_t *ht, hash_ctrl_t *type, void *privdata);

/* -------------------------- hash functions -------------------------------- */

/* Thomas Wang's 32 bit Mix Function */
unsigned int
hash_func_int(unsigned int key){
    key += ~(key << 15);
    key ^=  (key >> 10);
    key +=  (key << 3);
    key ^=  (key >> 6);
    key += ~(key << 11);
    key ^=  (key >> 16);
    return key;
}

/* Identity hash function for integer keys */
unsigned int
hash_func_identity(unsigned int key){
    return key;
}

/* Generic hash function (a popular one from Bernstein).
 * I tested a few and this was the best. */
unsigned int
hash_func_gen(const unsigned char *buf, int len) {
    unsigned int hash = 5381;

    while (len--)
        hash = ((hash << 5) + hash) + (*buf++); /* hash * 33 + c */
    return hash;
}

/* And a case insensitive version */
unsigned int
hash_func_gen_nocase(const unsigned char *buf, int len) {
    unsigned int hash = 5381;

    while (len--)
        hash = ((hash << 5) + hash) + (tolower(*buf++)); /* hash * 33 + c */
    return hash;
}

/* ----------------------------- API implementation ------------------------- */

/* Reset an hashtable already initialized with ht_init().
 * NOTE: This function should only called by ht_destroy(). */
static void
_hash_reset(hash_table_t *ht){
    ht->table = NULL;
    ht->size = 0;
    ht->sizemask = 0;
    ht->used = 0;
}

/* Hash table types */

hash_ctrl_t hash_ctrl_copy_key_string = {
	hash_func_string,           /* hash function */
	hash_dup_string,            /* key dup */
	NULL,                       /* val dup */
	hash_cmp_string,   			/* key compare */
	hash_destroy_string,        /* key destructor */
	NULL                        /* val destructor */
};

hash_ctrl_t hash_ctrl_copy_key_val_string = {
	hash_func_string,           /* hash function */
	hash_dup_string,            /* key dup */
	hash_dup_string,            /* val dup */
	hash_cmp_string,   			/* key compare */
	hash_destroy_string,        /* key destructor */
	hash_destroy_string         /* val destructor */
};
 
/* Create a new hash table */
hash_t *
hash_new(hash_ctrl_t *type, void *privdata) {
    hash_t *d = (hash_t*)malloc(sizeof(*d));

    if(!type) type = &hash_ctrl_copy_key_string;

    _hash_init(d,type,privdata);
    return d;
}

/* Initialize the hash table */
int
_hash_init(hash_t *d, hash_ctrl_t *type, void *privdata){
    _hash_reset(&d->ht[0]);
    _hash_reset(&d->ht[1]);
    d->type = type;
    d->privdata = privdata;
    d->rehashidx = -1;
    d->iterators = 0;
    return HASH_OK;
}

/* Resize the table to the minimal size that contains all the elements,
 * but with the invariant of a USER/BUCKETS ratio near to <= 1 */
int
hash_resize(hash_t *d){
    int minimal;

    if (!dict_can_resize || hash_is_rehashing(d)) return HASH_ERR;
    minimal = d->ht[0].used;
    if (minimal < HASH_HT_INITIAL_SIZE)
        minimal = HASH_HT_INITIAL_SIZE;
    return hash_expand(d, minimal);
}

/* Expand or create the hashtable */
int
hash_expand(hash_t *d, unsigned long size){
    hash_table_t n; /* the new hashtable */
    unsigned long realsize = _hash_next_power(size);

    /* the size is invalid if it is smaller than the number of
     * elements already inside the hashtable */
    if (hash_is_rehashing(d) || d->ht[0].used > size)
        return HASH_ERR;

    /* Allocate the new hashtable and initialize all pointers to NULL */
    n.size = realsize;
    n.sizemask = realsize-1;
    n.table = (hash_entry_t**)calloc(1, realsize*sizeof(hash_entry_t*));
    n.used = 0;

    /* Is this the first initialization? If so it's not really a rehashing
     * we just set the first hash table so that it can accept keys. */
    if (d->ht[0].table == NULL) {
        d->ht[0] = n;
        return HASH_OK;
    }

    /* Prepare a second hash table for incremental rehashing */
    d->ht[1] = n;
    d->rehashidx = 0;
    return HASH_OK;
}

/* Performs N steps of incremental rehashing. Returns 1 if there are still
 * keys to move from the old to the new hash table, otherwise 0 is returned.
 * Note that a rehashing step consists in moving a bucket (that may have more
 * than one key as we use chaining) from the old to the new hash table. */
int
hash_rehash(hash_t *d, int n) {
    if (!hash_is_rehashing(d)) return 0;

    while(n--) {
        hash_entry_t *de, *nextde;

        /* Check if we already rehashed the whole table... */
        if (d->ht[0].used == 0) {
            free(d->ht[0].table);
            d->ht[0] = d->ht[1];
            _hash_reset(&d->ht[1]);
            d->rehashidx = -1;
            return 0;
        }

        /* Note that rehashidx can't overflow as we are sure there are more
         * elements because ht[0].used != 0 */
        while(d->ht[0].table[d->rehashidx] == NULL) d->rehashidx++;
        de = d->ht[0].table[d->rehashidx];
        /* Move all the keys in this bucket from the old to the new hash HT */
        while(de) {
            unsigned int h;

            nextde = de->next;
            /* Get the index in the new hash table */
            h = hash_key(d, de->key) & d->ht[1].sizemask;
            de->next = d->ht[1].table[h];
            d->ht[1].table[h] = de;
            d->ht[0].used--;
            d->ht[1].used++;
            de = nextde;
        }
        d->ht[0].table[d->rehashidx] = NULL;
        d->rehashidx++;
    }
    return 1;
}

long long
time_in_millis(void) {
	#if defined (__UNIX__)
    struct timeval tv;
    gettimeofday (&tv, NULL);

    return (int64_t) ((int64_t) tv.tv_sec * 1000 + (int64_t) tv.tv_usec / 1000);
	#elif (defined (__WINDOWS__))
		FILETIME ft;
		GetSystemTimeAsFileTime (&ft);
		return (int64_t) (*((int64_t *) (&ft)) / 10000);
	#endif
}

/* Rehash for an amount of time between ms milliseconds and ms+1 milliseconds */
int
hash_rehash_millis(hash_t *d, int ms) {
    long long start = time_in_millis();
    int rehashes = 0;

    while(hash_rehash(d,100)) {
        rehashes += 100;
        if (time_in_millis()-start > ms) break;
    }
    return rehashes;
}

/* This function performs just a step of rehashing, and only if there are
 * no safe iterators bound to our hash table. When we have iterators in the
 * middle of a rehashing we can't mess with the two hash tables otherwise
 * some element can be missed or duplicated.
 *
 * This function is called by common lookup or update operations in the
 * dictionary so that the hash table automatically migrates from H1 to H2
 * while it is actively used. */
static void _hash_rehash_step(hash_t *d) {
    if (d->iterators == 0) hash_rehash(d,1);
}

/* Add an element to the target hash table */
int
hash_add(hash_t *d, void *key, void *val){
    int index;
    hash_entry_t *entry;
    hash_table_t *ht;

    if (hash_is_rehashing(d)) _hash_rehash_step(d);

    /* Get the index of the new element, or -1 if
     * the element already exists. */
    if ((index = _hash_key_index(d, key)) == -1)
        return HASH_ERR;

    /* Allocates the memory and stores key */
    ht = hash_is_rehashing(d) ? &d->ht[1] : &d->ht[0];
    entry = (hash_entry_t*)malloc(sizeof(*entry));
    entry->next = ht->table[index];
    ht->table[index] = entry;
    ht->used++;

    /* Set the hash entry fields. */
    hash_entry_set_key(d, entry, key);
    hash_entry_set_val(d, entry, val);
    return HASH_OK;
}

/* Add an element, discarding the old if the key already exists.
 * Return 1 if the key was added from scratch, 0 if there was already an
 * element with such key and dictReplace() just performed a value update
 * operation. */
int
hash_put(hash_t *d, void *key, void *val){
    hash_entry_t *entry, auxentry;

    /* Try to add the element. If the key
     * does not exists dictAdd will suceed. */
    if (hash_add(d, key, val) == HASH_OK)
        return 1;
    /* It already exists, get the entry */
    entry = hash_entry(d, key);
    /* Free the old value and set the new one */
    /* Set the new value and free the old one. Note that it is important
     * to do that in this order, as the value may just be exactly the same
     * as the previous one. In this context, think to reference counting,
     * you want to increment (set), and then decrement (free), and not the
     * reverse. */
    auxentry = *entry;
    hash_entry_set_val(d, entry, val);
    hash_entry_free_val(d, &auxentry);
    return 0;
}

/* Search and remove an element */
static int
hash_generic_delete(hash_t *d, const void *key, int nofree){
    unsigned int h, idx;
    hash_entry_t *he, *prevHe;
    int table;

    if (d->ht[0].size == 0) return HASH_ERR; /* d->ht[0].table is NULL */
    if (hash_is_rehashing(d)) _hash_rehash_step(d);
    h = hash_key(d, key);

    for (table = 0; table <= 1; table++) {
        idx = h & d->ht[table].sizemask;
        he = d->ht[table].table[idx];
        prevHe = NULL;
        while(he) {
            if (hash_key_compare(d, key, he->key)) {
                /* Unlink the element from the list */
                if (prevHe)
                    prevHe->next = he->next;
                else
                    d->ht[table].table[idx] = he->next;
                if (!nofree) {
                    hash_entry_free_key(d, he);
                    hash_entry_free_val(d, he);
                }
                free(he);
                d->ht[table].used--;
                return HASH_OK;
            }
            prevHe = he;
            he = he->next;
        }
        if (!hash_is_rehashing(d)) break;
    }
    return HASH_ERR; /* not found */
}

int
hash_rem(hash_t *ht, const void *key) {
    return hash_generic_delete(ht,key,0);
}

int
hash_rem_nofree(hash_t *ht, const void *key) {
    return hash_generic_delete(ht,key,1);
}

/* Destroy an entire dictionary */
int
_hash_clear(hash_t *d, hash_table_t *ht){
    unsigned long i;

    /* Free all the elements */
    for (i = 0; i < ht->size && ht->used > 0; i++) {
        hash_entry_t *he, *nextHe;

        if ((he = ht->table[i]) == NULL) continue;
        while(he) {
            nextHe = he->next;
            hash_entry_free_key(d, he);
            hash_entry_free_val(d, he);
            free(he);
            ht->used--;
            he = nextHe;
        }
    }
    /* Free the table and the allocated cache structure */
    free(ht->table);
    /* Re-initialize the table */
    _hash_reset(ht);
    return HASH_OK; /* never fails */
}

/* Clear & Release the hash table */
void
hash_destroy(hash_t ** self_p){
	hash_t* self;
	if(!self_p) return;
	self = *self_p;
	if(self){
		_hash_clear(self,&self->ht[0]);
		_hash_clear(self,&self->ht[1]);
		free(self);
		*self_p = NULL;
	}
}

hash_entry_t *
hash_entry(hash_t *d, const void *key){
    hash_entry_t *he;
    unsigned int h, idx, table;

    if (d->ht[0].size == 0) return NULL; /* We don't have a table at all */
    if (hash_is_rehashing(d)) _hash_rehash_step(d);
    h = hash_key(d, key);
    for (table = 0; table <= 1; table++) {
        idx = h & d->ht[table].sizemask;
        he = d->ht[table].table[idx];
        while(he) {
            if (hash_key_compare(d, key, he->key))
                return he;
            he = he->next;
        }
        if (!hash_is_rehashing(d)) return NULL;
    }
    return NULL;
}

void *
hash_get(hash_t *d, const void *key) {
    hash_entry_t *he;

    he = hash_entry(d,key);
    return he ? hash_entry_val(he) : NULL;
}

hash_iter_t *
hash_iter_new(hash_t *d){
    hash_iter_t *iter = (hash_iter_t*)malloc(sizeof(*iter));

    iter->d = d;
    iter->table = 0;
    iter->index = -1;
    iter->safe = 0;
    iter->entry = NULL;
    iter->next_entry = NULL;
    return iter;
}

hash_iter_t *
hash_iter_new_safe(hash_t *d) {
    hash_iter_t *i = hash_iter_new(d);

    i->safe = 1;
    return i;
}

hash_entry_t*
hash_iter_next(hash_iter_t *iter){
    while (1) {
        if (iter->entry == NULL) {
            hash_table_t *ht = &iter->d->ht[iter->table];
            if (iter->safe && iter->index == -1 && iter->table == 0)
                iter->d->iterators++;
            iter->index++;
            if (iter->index >= (signed) ht->size) {
                if (hash_is_rehashing(iter->d) && iter->table == 0) {
                    iter->table++;
                    iter->index = 0;
                    ht = &iter->d->ht[1];
                } else {
                    break;
                }
            }
            iter->entry = ht->table[iter->index];
        } else {
            iter->entry = iter->next_entry;
        }
        if (iter->entry) {
            /* We need to save the 'next' here, the iterator user
             * may delete the entry we are returning. */
            iter->next_entry = iter->entry->next;
            return iter->entry;
        }
    }
    return NULL;
}

void
hash_iter_destroy(hash_iter_t **iter_p){
	hash_iter_t* iter;
	if(!iter_p) return;
	iter = *iter_p;
	if(iter){
		if (iter->safe && !(iter->index == -1 && iter->table == 0))
			iter->d->iterators--;
		free(iter);
		*iter_p = NULL;
	}
}

/* Return a random entry from the hash table. Useful to
 * implement randomized algorithms */
hash_entry_t *
hash_random(hash_t *d){
    hash_entry_t *he, *orighe;
    unsigned int h;
    int listlen, listele;

    if (hash_size(d) == 0) return NULL;
    if (hash_is_rehashing(d)) _hash_rehash_step(d);
    if (hash_is_rehashing(d)) {
        do {
            h = random() % (d->ht[0].size+d->ht[1].size);
            he = (h >= d->ht[0].size) ? d->ht[1].table[h - d->ht[0].size] :
                                      d->ht[0].table[h];
        } while(he == NULL);
    } else {
        do {
            h = random() & d->ht[0].sizemask;
            he = d->ht[0].table[h];
        } while(he == NULL);
    }

    /* Now we found a non empty bucket, but it is a linked
     * list and we need to get a random element from the list.
     * The only sane way to do so is counting the elements and
     * select a random index. */
    listlen = 0;
    orighe = he;
    while(he) {
        he = he->next;
        listlen++;
    }
    listele = random() % listlen;
    he = orighe;
    while(listele--) he = he->next;
    return he;
}

/* ------------------------- private functions ------------------------------ */

/* Expand the hash table if needed */
static int
_hash_expand_if_needed(hash_t *d){
    /* Incremental rehashing already in progress. Return. */
    if (hash_is_rehashing(d)) return HASH_OK;

    /* If the hash table is empty expand it to the intial size. */
    if (d->ht[0].size == 0) return hash_expand(d, HASH_HT_INITIAL_SIZE);

    /* If we reached the 1:1 ratio, and we are allowed to resize the hash
     * table (global setting) or we should avoid it but the ratio between
     * elements/buckets is over the "safe" threshold, we resize doubling
     * the number of buckets. */
    if (d->ht[0].used >= d->ht[0].size &&
        (dict_can_resize ||
         d->ht[0].used/d->ht[0].size > dict_force_resize_ratio))
    {
        return hash_expand(d, ((d->ht[0].size > d->ht[0].used) ?
                                    d->ht[0].size : d->ht[0].used)*2);
    }
    return HASH_OK;
}

/* Our hash table capability is a power of two */
static unsigned long
_hash_next_power(unsigned long size){
    unsigned long i = HASH_HT_INITIAL_SIZE;

    if (size >= LONG_MAX) return LONG_MAX;
    while(1) {
        if (i >= size)
            return i;
        i *= 2;
    }
    return 0; //should not run here
}

/* Returns the index of a free slot that can be populated with
 * an hash entry for the given 'key'.
 * If the key already exists, -1 is returned.
 *
 * Note that if we are in the process of rehashing the hash table, the
 * index is always returned in the context of the second (new) hash table. */
static int
_hash_key_index(hash_t *d, const void *key){
    unsigned int h, idx, table;
    hash_entry_t *he;

    /* Expand the hashtable if needed */
    if (_hash_expand_if_needed(d) == HASH_ERR)
        return -1;
    /* Compute the key hash value */
    h = hash_key(d, key);
    for (table = 0; table <= 1; table++) {
        idx = h & d->ht[table].sizemask;
        /* Search if this slot does not already contain the given key */
        he = d->ht[table].table[idx];
        while(he) {
            if (hash_key_compare(d, key, he->key))
                return -1;
            he = he->next;
        }
        if (!hash_is_rehashing(d)) break;
    }
    return idx;
}

void
hash_clear(hash_t *d) {
    _hash_clear(d,&d->ht[0]);
    _hash_clear(d,&d->ht[1]);
    d->rehashidx = -1;
    d->iterators = 0;
}

#define DICT_STATS_VECTLEN 50
static void
_hash_print_stats_ht(hash_table_t *ht) {
    unsigned long i, slots = 0, chainlen, maxchainlen = 0;
    unsigned long totchainlen = 0;
    unsigned long clvector[DICT_STATS_VECTLEN];

    if (ht->used == 0) {
        printf("No stats available for empty dictionaries\n");
        return;
    }

    for (i = 0; i < DICT_STATS_VECTLEN; i++) clvector[i] = 0;
    for (i = 0; i < ht->size; i++) {
        hash_entry_t *he;

        if (ht->table[i] == NULL) {
            clvector[0]++;
            continue;
        }
        slots++;
        /* For each hash entry on this slot... */
        chainlen = 0;
        he = ht->table[i];
        while(he) {
            chainlen++;
            he = he->next;
        }
        clvector[(chainlen < DICT_STATS_VECTLEN) ? chainlen : (DICT_STATS_VECTLEN-1)]++;
        if (chainlen > maxchainlen) maxchainlen = chainlen;
        totchainlen += chainlen;
    }
    printf("Hash table stats:\n");
    printf(" table size: %ld\n", ht->size);
    printf(" number of elements: %ld\n", ht->used);
    printf(" different slots: %ld\n", slots);
    printf(" max chain length: %ld\n", maxchainlen);
    printf(" avg chain length (counted): %.02f\n", (float)totchainlen/slots);
    printf(" avg chain length (computed): %.02f\n", (float)ht->used/slots);
    printf(" Chain length distribution:\n");
    for (i = 0; i < DICT_STATS_VECTLEN-1; i++) {
        if (clvector[i] == 0) continue;
        printf("   %s%ld: %ld (%.02f%%)\n",(i == DICT_STATS_VECTLEN-1)?">= ":"", i, clvector[i], ((float)clvector[i]/ht->size)*100);
    }
}

void
hash_stats(hash_t *d) {
    _hash_print_stats_ht(&d->ht[0]);
    if (hash_is_rehashing(d)) {
        printf("-- Rehashing into ht[1]:\n");
        _hash_print_stats_ht(&d->ht[1]);
    }
}

void
hash_enable_resize(void) {
    dict_can_resize = 1;
}

void
hash_disable_resize(void) {
    dict_can_resize = 0;
}

