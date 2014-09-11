#ifndef __ZBOX_MSG_H_
#define __ZBOX_MSG_H_

#include "platform.h"  
#ifdef __cplusplus
extern "C" {
#endif

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
ZBOX_EXPORT void* msg_get_body(msg_t* self);
ZBOX_EXPORT int   msg_get_body_len(msg_t* self);
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
 

#ifdef __cplusplus
}
#endif

#endif
