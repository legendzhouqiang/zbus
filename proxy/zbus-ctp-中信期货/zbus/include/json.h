#ifndef __JSON__H__
#define __JSON__H__ 

#include "platform.h"

#ifdef __cplusplus
extern "C"
{
#endif

typedef struct json json_t;

#define JSON_FALSE 0
#define JSON_TRUE 1
#define JSON_NULL 2
#define JSON_NUMBER 3
#define JSON_STRING 4
#define JSON_ARRAY 5
#define JSON_OBJECT 6

#define JSON_IsReference 256

/* The cJSON structure: */
struct json {
	struct json *next,*prev;	/* next/prev allow you to walk array/object chains. Alternatively, use GetArraySize/GetArrayItem/GetObjectItem */
	struct json *child;		/* An array or object item will have a child pointer pointing to a chain of the items in the array/object. */

	int type;					/* The type of the item, as above. */

	char *valuestring;			/* The item's string, if type==cJSON_String */
	int valueint;				/* The item's number, if type==cJSON_Number */
	double valuedouble;			/* The item's number, if type==cJSON_Number */

	char *string;				/* The item's name string, if this item is the child of, or is in the list of subitems of an object. */
};


typedef struct json_hook {
	void *(*malloc_fn)(size_t);
	void (*free_fn)(void *);
} json_hook_t;

/* Supply malloc, realloc and free functions to cJSON */
ZBOX_EXPORT void json_init_hooks(json_hook_t* hooks);


/* Supply a block of JSON, and this returns a cJSON object you can interrogate. Call cJSON_Delete when finished. */
ZBOX_EXPORT json_t *json_parse(const char *value);
/* ParseWithOpts allows you to require (and check) that the JSON is null terminated, and to retrieve the pointer to the final byte parsed. */
ZBOX_EXPORT json_t *json_parse_ext(const char *value,const char **return_parse_end,int require_null_terminated);

/* Render a cJSON entity to text for transfer/storage. Free the char* when finished. */
ZBOX_EXPORT char  *json_dump(json_t *item);
/* Render a cJSON entity to text for transfer/storage without any formatting. Free the char* when finished. */
ZBOX_EXPORT char  *json_dump_raw(json_t *item);
/* Delete a cJSON entity and all subentities. */
ZBOX_EXPORT void   json_destroy(json_t *c);

/* Returns the number of items in an array (or object). */
ZBOX_EXPORT int	  json_array_size(json_t *array);
/* Retrieve item number "item" from array "array". Returns NULL if unsuccessful. */
ZBOX_EXPORT json_t *json_array_item(json_t *array,int item);
/* Get item "string" from object. Case insensitive. */
ZBOX_EXPORT json_t *json_object_item(json_t *object,const char *string);

/* For analysing failed parses. This returns a pointer to the parse error. You'll probably need to look a few chars back to make sense of it. Defined when cJSON_Parse() returns 0. 0 when cJSON_Parse() succeeds. */
ZBOX_EXPORT const char *json_error(void);

/* These calls create a cJSON item of the appropriate type. */
ZBOX_EXPORT json_t *json_null(void);
ZBOX_EXPORT json_t *json_true(void);
ZBOX_EXPORT json_t *json_false(void);
ZBOX_EXPORT json_t *json_bool(int b);
ZBOX_EXPORT json_t *json_number(double num);
ZBOX_EXPORT json_t *json_string(const char *string);
ZBOX_EXPORT json_t *json_array(void);
ZBOX_EXPORT json_t *json_object(void);

/* These utilities create an Array of count items. */
ZBOX_EXPORT json_t *json_array_int(int *numbers,int count);
ZBOX_EXPORT json_t *json_array_float(float *numbers,int count);
ZBOX_EXPORT json_t *json_array_double(double *numbers,int count);
ZBOX_EXPORT json_t *json_array_string(const char **strings,int count);

/* Append item to the specified array/object. */
ZBOX_EXPORT void json_array_add(json_t *array, json_t *item);
ZBOX_EXPORT void	json_object_add(json_t *object,const char *string,json_t *item);
/* Append reference to item to the specified array/object. Use this when you want to add an existing cJSON to a new cJSON, but don't want to corrupt your existing cJSON. */
ZBOX_EXPORT void json_array_addref(json_t *array, json_t *item);
ZBOX_EXPORT void	json_object_addref(json_t *object,const char *string,json_t *item);

/* Remove/Detatch items from Arrays/Objects. */
ZBOX_EXPORT json_t* json_array_detach(json_t *array,int which);
ZBOX_EXPORT void    json_array_delete(json_t *array,int which);
ZBOX_EXPORT json_t* json_object_detach(json_t *object,const char *string);
ZBOX_EXPORT void    json_object_delete(json_t *object,const char *string);

/* Update array items. */
ZBOX_EXPORT void json_array_replace(json_t *array,int which,json_t *newitem);
ZBOX_EXPORT void json_object_replace(json_t *object,const char *string,json_t *newitem);

/* Duplicate a cJSON item */
ZBOX_EXPORT json_t *json_dup(json_t *item,int recurse);
/* Duplicate will create a new, identical cJSON item to the one you pass, in new memory that will
need to be released. With recurse!=0, it will duplicate any children connected to the item.
The item->next and ->prev pointers are always zero on return from Duplicate. */

/* Macros for creating things quickly. */
#define json_object_addnull(object,name)	json_object_add(object, name, json_null())
#define json_object_addbool(object,name,b)	json_object_add(object, name, json_bool(b))
#define json_object_addnum(object,name,n)	json_object_add(object, name, json_number(n))
#define json_object_addstr(object,name,s)	json_object_add(object, name, json_string(s))

/* When assigning an integer value, it needs to be propagated to valuedouble too. */
#define json_object_setint(object,val)			((object)?(object)->valueint=(object)->valuedouble=(val):(val))


#define BASE64_ERR_BUFFER_TOO_SMALL				-0x002A
#define BASE64_ERR_INVALID_CHARACTER            -0x002C

extern int base64_encode( unsigned char *dst, size_t *dlen,
				  const unsigned char *src, size_t slen );


#ifdef __cplusplus
}
#endif

#endif
