#ifndef __ZBUS_H__
#define __ZBUS_H__

#include "platform.h"   
#ifdef __cplusplus
extern "C" {
#endif
 
/////////////////////////////////////////
//下面的常量定义都不是协议必须要求的，只是常规应用定义的，ZBUS中大量采用放在这里

#define HEARTBEAT          "heartbeat" //心跳消息

//标准HTTP头部内容
#define HEADER_CLIENT      "remote-addr"
#define HEADER_ENCODING    "content-encoding"

#define HEADER_BROKER      "broker"
#define HEADER_TOPIC       "topic"   //使用,分隔 
#define HEADER_MQ_REPLY    "mq-reply"
#define HEADER_MQ          "mq"
#define HEADER_TOKEN       "token"
#define HEADER_MSGID       "msgid"	
#define HEADER_MSGID_RAW   "msgid-raw"	
#define HEADER_ACK         "ack"
#define HEADER_REPLY_CODE  "reply-code"	


typedef struct meta meta_t;
typedef struct msg msg_t;
typedef struct iobuf iobuf_t; 


//兼容HTTP协议，元数据行（头部第一行）+头部+消息体操作
ZBOX_EXPORT msg_t*msg_new (void); 
ZBOX_EXPORT void  msg_destroy (msg_t **self_p);
ZBOX_EXPORT char* msg_get_head (msg_t *self, char* key);
ZBOX_EXPORT void  msg_set_head (msg_t *self, char* key, char* value);
ZBOX_EXPORT char* msg_get_head_or_param(msg_t* self, char* key);
ZBOX_EXPORT void  msg_set_body_nocopy(msg_t* self, void* body, int len);
ZBOX_EXPORT void  msg_set_body_copy(msg_t* self, void* body, int len);
ZBOX_EXPORT void  msg_set_bodyfmt(msg_t* self, const char* format, ...);
ZBOX_EXPORT void  msg_set_body(msg_t* self, char* body);
ZBOX_EXPORT char* msg_get_command(msg_t* self);
ZBOX_EXPORT void  msg_set_command(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_status(msg_t* self);
ZBOX_EXPORT void  msg_set_status(msg_t* self, char* value); 
ZBOX_EXPORT int   msg_is_status200(msg_t* self);
ZBOX_EXPORT int   msg_is_status404(msg_t* self);
ZBOX_EXPORT int   msg_is_status500(msg_t* self);

//ZBUS中可能用到一些头部扩展操作，便捷扩展，都是GET_HEAD的便捷方法封装
ZBOX_EXPORT char* msg_get_sender(msg_t* self);
ZBOX_EXPORT void  msg_set_sender(msg_t* self, char* value); 
ZBOX_EXPORT char* msg_get_mq_reply(msg_t* self);
ZBOX_EXPORT void  msg_set_mq_reply(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_msgid(msg_t* self);
ZBOX_EXPORT void  msg_set_msgid(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_msgid_raw(msg_t* self);
ZBOX_EXPORT void  msg_set_msgid_raw(msg_t* self, char* value);
ZBOX_EXPORT bool  msg_is_ack(msg_t* self);
ZBOX_EXPORT void  msg_set_ack(msg_t* self, bool value);
ZBOX_EXPORT char* msg_get_mq(msg_t* self);
ZBOX_EXPORT void  msg_set_mq(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_token(msg_t* self);
ZBOX_EXPORT void  msg_set_token(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_topic(msg_t* self);
ZBOX_EXPORT void  msg_set_topic(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_encoding(msg_t* self);
ZBOX_EXPORT void  msg_set_encoding(msg_t* self, char* value);

ZBOX_EXPORT void   msg_encode(msg_t* self, iobuf_t* buf);
ZBOX_EXPORT msg_t* msg_decode(iobuf_t* buf);
ZBOX_EXPORT void   msg_print(msg_t* self);
ZBOX_EXPORT char*  msg_copy_body(msg_t* self);
//META处理
ZBOX_EXPORT meta_t* meta_new(char* meta);
ZBOX_EXPORT void    meta_destroy(meta_t** self_p);
ZBOX_EXPORT char*   meta_get_param(meta_t* self, char* key);
ZBOX_EXPORT void    meta_set_param(meta_t* self, char* key, char* val);
ZBOX_EXPORT meta_t* meta_parse(char* meta);
ZBOX_EXPORT void    meta_encode(meta_t* self, iobuf_t* buf);

 
struct iobuf{
	int mark;
	int position;
	int limit;
	int capacity; 
	int own_data;
	char* data; 
};

//IO缓存操作
ZBOX_EXPORT iobuf_t* iobuf_new(int capacity);
ZBOX_EXPORT iobuf_t* iobuf_dup(iobuf_t* buf);
ZBOX_EXPORT iobuf_t* iobuf_wrap(char array[], int len); 
ZBOX_EXPORT void     iobuf_destroy(iobuf_t** self_p); 

ZBOX_EXPORT void iobuf_mark(iobuf_t* self);
ZBOX_EXPORT void iobuf_reset(iobuf_t* self);
ZBOX_EXPORT int  iobuf_remaining(iobuf_t* self);
ZBOX_EXPORT int  iobuf_drain(iobuf_t* self, int n);
ZBOX_EXPORT int  iobuf_copyout(iobuf_t* self, char data[], int len);
ZBOX_EXPORT int  iobuf_get(iobuf_t* self, char data[],int len);
ZBOX_EXPORT int  iobuf_put(iobuf_t* self, void* data, int len);
ZBOX_EXPORT int  iobuf_putstr(iobuf_t* self, char* str);
ZBOX_EXPORT int  iobuf_putkv(iobuf_t* self, char* key, char* val);
ZBOX_EXPORT int  iobuf_putbuf(iobuf_t* self, iobuf_t* buf); 

ZBOX_EXPORT char*    iobuf_begin(iobuf_t* self);
ZBOX_EXPORT char*    iobuf_end(iobuf_t* self);
ZBOX_EXPORT iobuf_t* iobuf_flip(iobuf_t* self);
ZBOX_EXPORT int      iobuf_mv(iobuf_t* self, int n);
ZBOX_EXPORT iobuf_t* iobuf_limit(iobuf_t* self, int new_limit);
ZBOX_EXPORT void     iobuf_print(iobuf_t* self);
 
/////////////////////////////////////////
typedef struct rclient rclient_t;
ZBOX_EXPORT rclient_t* rclient_new(char* broker);
ZBOX_EXPORT void       rclient_destroy(rclient_t** self_p);
ZBOX_EXPORT rclient_t* rclient_connect(char* broker, int auto_reconnect_millis);
ZBOX_EXPORT bool       rclient_reconnect(rclient_t* self, int reconnect_msecs); 

ZBOX_EXPORT int  rclient_send(rclient_t* self, msg_t* msg);
ZBOX_EXPORT int  rclient_recv(rclient_t* self, msg_t** msg_p, int timeout);
ZBOX_EXPORT int  rclient_invoke(rclient_t* self, msg_t* req, msg_t** res_p, int timeout);

/////////////////JSON////////////////////

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
typedef struct json {
	struct json *next,*prev;	/* next/prev allow you to walk array/object chains. Alternatively, use GetArraySize/GetArrayItem/GetObjectItem */
	struct json *child;		/* An array or object item will have a child pointer pointing to a chain of the items in the array/object. */

	int type;					/* The type of the item, as above. */

	char *valuestring;			/* The item's string, if type==cJSON_String */
	int valueint;				/* The item's number, if type==cJSON_Number */
	double valuedouble;			/* The item's number, if type==cJSON_Number */

	char *string;				/* The item's name string, if this item is the child of, or is in the list of subitems of an object. */
} json_t;



typedef struct json_hook {
	void *(*malloc_fn)(size_t);
	void (*free_fn)(void *);
} json_hook_t;

/* Supply malloc, realloc and free functions to cJSON */
extern void json_init_hooks(json_hook_t* hooks);


/* Supply a block of JSON, and this returns a cJSON object you can interrogate. Call cJSON_Delete when finished. */
extern json_t *json_parse(const char *value);
/* ParseWithOpts allows you to require (and check) that the JSON is null terminated, and to retrieve the pointer to the final byte parsed. */
extern json_t *json_parse_ext(const char *value,const char **return_parse_end,int require_null_terminated);

/* Render a cJSON entity to text for transfer/storage. Free the char* when finished. */
extern char  *json_dump(json_t *item);
/* Render a cJSON entity to text for transfer/storage without any formatting. Free the char* when finished. */
extern char  *json_dump_raw(json_t *item);
/* Delete a cJSON entity and all subentities. */
extern void   json_destroy(json_t *c);

/* Returns the number of items in an array (or object). */
extern int	  json_array_size(json_t *array);
/* Retrieve item number "item" from array "array". Returns NULL if unsuccessful. */
extern json_t *json_array_item(json_t *array,int item);
/* Get item "string" from object. Case insensitive. */
extern json_t *json_object_item(json_t *object,const char *string);

/* For analysing failed parses. This returns a pointer to the parse error. You'll probably need to look a few chars back to make sense of it. Defined when cJSON_Parse() returns 0. 0 when cJSON_Parse() succeeds. */
extern const char *json_error(void);

/* These calls create a cJSON item of the appropriate type. */
extern json_t *json_null(void);
extern json_t *json_true(void);
extern json_t *json_false(void);
extern json_t *json_bool(int b);
extern json_t *json_number(double num);
extern json_t *json_string(const char *string);
extern json_t *json_array(void);
extern json_t *json_object(void);

/* These utilities create an Array of count items. */
extern json_t *json_array_int(int *numbers,int count);
extern json_t *json_array_float(float *numbers,int count);
extern json_t *json_array_double(double *numbers,int count);
extern json_t *json_array_string(const char **strings,int count);

/* Append item to the specified array/object. */
extern void json_array_add(json_t *array, json_t *item);
extern void	json_object_add(json_t *object,const char *string,json_t *item);
/* Append reference to item to the specified array/object. Use this when you want to add an existing cJSON to a new cJSON, but don't want to corrupt your existing cJSON. */
extern void json_array_addref(json_t *array, json_t *item);
extern void	json_object_addref(json_t *object,const char *string,json_t *item);

/* Remove/Detatch items from Arrays/Objects. */
extern json_t* json_array_detach(json_t *array,int which);
extern void    json_array_delete(json_t *array,int which);
extern json_t* json_object_detach(json_t *object,const char *string);
extern void    json_object_delete(json_t *object,const char *string);

/* Update array items. */
extern void json_array_replace(json_t *array,int which,json_t *newitem);
extern void json_object_replace(json_t *object,const char *string,json_t *newitem);

/* Duplicate a cJSON item */
extern json_t *json_dup(json_t *item,int recurse);
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


/////////////////////////////////////////////////////////////////

#define PRODUCE       "produce"     //生产消息
#define CONSUME       "consume"     //消费消息   
#define REQUEST       "request"     //请求等待应答消息 
#define HEARTBEAT     "heartbeat"   //心跳消息  
#define ADMIN         "admin"       //管理类消息
#define CREATE_MQ     "create_mq"   
#define TRACK_REPORT  "track_report" 
#define TRACK_SUB     "track_sub" 
#define TRACK_PUB     "track_pub"

#define MODE_MQ		  1
#define MODE_PUBSUB   2 
#define MODE_TEMP     4

typedef msg_t* (service_handler)(msg_t* req, void* privdata);
typedef void   (msg_callback)(msg_t* msg, void* privdata);
typedef struct rpc_cfg{
	char  broker[256];
	char  mq[256];
	char  acc_token[256];
	char  reg_token[256];
	int   thread_count;
	int   consume_timeout;
	int   reconnect_interval;
	service_handler* handler;
}rpc_cfg_t;

typedef struct track_agent_cfg{
	char  seed_broker[256];
	char  track_server[256]; 
	int   query_interval;
	int   pool_max_clients;
	int   pool_timeout; 
}track_agent_cfg_t;


typedef struct producer producer_t;
typedef struct consumer consumer_t;

typedef struct rpc rpc_t;
typedef struct jsonrpc jsonrpc_t;

ZBOX_EXPORT producer_t* producer_new(rclient_t* client, char* mq, int mode);
ZBOX_EXPORT void producer_destroy(producer_t** self_p);
ZBOX_EXPORT void producer_set_token(producer_t* self, char* token);
ZBOX_EXPORT int  producer_send(producer_t* self, msg_t* msg, msg_t** result_p, int timeout);

ZBOX_EXPORT consumer_t* consumer_new(rclient_t* client, char* mq, int mode);
ZBOX_EXPORT void consumer_destroy(consumer_t** self_p);
ZBOX_EXPORT void consumer_set_acc_token(consumer_t* self, char* value);
ZBOX_EXPORT void consumer_set_reg_token(consumer_t* self, char* value);
ZBOX_EXPORT void consumer_set_topic(consumer_t* self, char* value);
ZBOX_EXPORT int  consumer_recv(consumer_t* self, msg_t** result_p, int timeout);
ZBOX_EXPORT int  consumer_reply(consumer_t* self, msg_t* msg);

ZBOX_EXPORT rpc_t* rpc_new(rclient_t* client, char* mq);
ZBOX_EXPORT void   rpc_destroy(rpc_t** self_p);
ZBOX_EXPORT void   rpc_set_token(rpc_t* self, char* value);
ZBOX_EXPORT void   rpc_set_encoding(rpc_t* self, char* value);
ZBOX_EXPORT int    rpc_invoke(rpc_t* self, msg_t* request, msg_t** result_p, int timeout);

ZBOX_EXPORT jsonrpc_t* jsonrpc_new(rclient_t* client, char* mq);
ZBOX_EXPORT void       jsonrpc_destroy(jsonrpc_t** self_p);
ZBOX_EXPORT jsonrpc_t* jsonrpc_set_token(jsonrpc_t* self, char* value);
ZBOX_EXPORT int  jsonrpc_invoke(jsonrpc_t* self, char* method, json_t* params, json_t** result_p, int timeout);
ZBOX_EXPORT int  jsonrpc_call(jsonrpc_t* self, json_t* request, json_t** result_p, int timeout);

ZBOX_EXPORT rpc_cfg_t* rpc_cfg_new();
ZBOX_EXPORT void rpc_cfg_destroy(rpc_cfg_t** self_p);
ZBOX_EXPORT void*  rpc_serve(void* args);//args: rpc_cfg_t


ZBOX_EXPORT msg_t* pack_json_request(json_t* request);
ZBOX_EXPORT json_t* unpack_json_object(msg_t* msg);


#ifdef __cplusplus
}
#endif

#endif
