#ifndef __ZBOX_MSG_H_
#define __ZBOX_MSG_H_

#include "platform.h" 
#include "buffer.h"
#ifdef __cplusplus
extern "C" {
#endif 

#define HEARTBEAT          "heartbeat"  
	 
#define HEADER_CLIENT			"remote-addr"
#define HEADER_CONTENT_TYPE		"content-type"

#define HEADER_CMD         "cmd" 
#define HEADER_SERVER      "server" 
#define HEADER_SENDER      "sender"
#define HEADER_RECVER      "recver"
#define HEADER_TOPIC          "topic" 
#define HEADER_ID          "id"	
#define HEADER_ORIGIN_ID   "orgin_id"	
#define HEADER_ACK         "ack" 
#define HEADER_ENCODING	   "encoding"
#define HEADER_REPLY_CODE  "reply_code"


typedef struct meta meta_t;
typedef struct msg msg_t;
typedef struct iobuf iobuf_t; 

 
ZBOX_EXPORT msg_t*msg_new (void); 
ZBOX_EXPORT void  msg_destroy (msg_t **self_p);
ZBOX_EXPORT char* msg_get_uri(msg_t* self);
ZBOX_EXPORT char* msg_get_path(msg_t* self); 
ZBOX_EXPORT char* msg_get_head (msg_t *self, char* key);
ZBOX_EXPORT void  msg_remove_head (msg_t *self, char* key);
ZBOX_EXPORT void  msg_set_head (msg_t *self, char* key, char* value); 
ZBOX_EXPORT void  msg_set_body_nocopy(msg_t* self, void* body, int len);
ZBOX_EXPORT void  msg_set_body_copy(msg_t* self, void* body, int len);
ZBOX_EXPORT void  msg_set_bodyfmt(msg_t* self, const char* format, ...);
ZBOX_EXPORT void  msg_set_body(msg_t* self, char* body);
ZBOX_EXPORT void  msg_set_json_body(msg_t* self, char* body);
ZBOX_EXPORT void* msg_get_body(msg_t* self);
ZBOX_EXPORT int   msg_get_body_len(msg_t* self);
ZBOX_EXPORT char* msg_get_status(msg_t* self);
ZBOX_EXPORT void  msg_set_status(msg_t* self, char* value); 
ZBOX_EXPORT int   msg_is_status200(msg_t* self);
ZBOX_EXPORT int   msg_is_status404(msg_t* self);
ZBOX_EXPORT int   msg_is_status500(msg_t* self);
 

ZBOX_EXPORT char* msg_get_cmd(msg_t* self);
ZBOX_EXPORT void  msg_set_cmd(msg_t* self, char* value);  
ZBOX_EXPORT char* msg_get_sender(msg_t* self);
ZBOX_EXPORT void  msg_set_sender(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_recver(msg_t* self);
ZBOX_EXPORT void  msg_set_recver(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_id(msg_t* self);
ZBOX_EXPORT void  msg_set_id(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_rawid(msg_t* self);
ZBOX_EXPORT void  msg_set_rawid(msg_t* self, char* value);
ZBOX_EXPORT bool  msg_is_ack(msg_t* self);
ZBOX_EXPORT void  msg_set_ack(msg_t* self, bool value);
ZBOX_EXPORT char* msg_get_topic(msg_t* self);
ZBOX_EXPORT void  msg_set_topic(msg_t* self, char* value); 
ZBOX_EXPORT char* msg_get_encoding(msg_t* self);
ZBOX_EXPORT void  msg_set_encoding(msg_t* self, char* value);
ZBOX_EXPORT char* msg_get_reply_code(msg_t* self);
ZBOX_EXPORT void  msg_set_reply_code(msg_t* self, char* value);

ZBOX_EXPORT void   msg_encode(msg_t* self, buf_t* buf);
ZBOX_EXPORT msg_t* msg_decode(buf_t* buf);
ZBOX_EXPORT void   msg_print(msg_t* self);
ZBOX_EXPORT char*  msg_copy_body(msg_t* self);

#ifdef __cplusplus
}
#endif

#endif
