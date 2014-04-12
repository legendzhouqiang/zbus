#ifndef __EVZMSG_H_
#define __EVZMSG_H_

#include "platform.h"
#include "msg.h" 
#include "hash.h"
#include <event2/buffer.h>

#ifdef __cplusplus
extern "C" {
#endif 

typedef struct zmsg zmsg_t;

#define HEAD_SPLIT "\r\n\r\n"


struct zmsg {
	char* sender;    //消息来源地址标识
	char* recver;    //消息目标地址标识
	char* msgid;     //消息ID(由发送者指定，后者消息队列内部设置）
	char* mq;		 //消息投递地址（消息队列标识）
	char* token;     //访问控制码
	
	char* command;   //消息的命令

	
	char* status;    //指定消息体的状态信息比如200,OK
	char* head;      //消息头部，动态可扩展, 增加Key-Value键值
	void* body;      //消息体
	size_t body_size;//消息体body大小


	hash_t* head_kvs;//内部使用，当head=null, 调用 head_set/get会触发head=NULL
};

zmsg_t* zmsg_new();
void zmsg_destroy(zmsg_t** self_p);

void zmsg_sender(zmsg_t* self, char* sender);
void zmsg_recver(zmsg_t* self, char* recver);
void zmsg_msgid(zmsg_t* self, char* msgid);
void zmsg_mq(zmsg_t* self, char* mq);
void zmsg_token(zmsg_t* self, char* token);

void zmsg_command(zmsg_t* self, char* command); 

void zmsg_status(zmsg_t* self, char* status);
void zmsg_body(zmsg_t* self, char* body);
void zmsg_body_blob(zmsg_t* self, void* body, size_t size);
void zmsg_body_nocopy(zmsg_t* self, void* body, size_t size);
void zmsg_print(zmsg_t*, FILE*);

char* zmsg_head_str(zmsg_t* self);
void  zmsg_head_set(zmsg_t* self, char* key, char* val);
char* zmsg_head_get(zmsg_t* self, char* key);


void evbuffer_add_frame(struct evbuffer* buf, frame_t* frame);
void evbuffer_add_msg(struct evbuffer* buf, msg_t* msg);
void evbuffer_add_zmsg(struct evbuffer* buf, zmsg_t* zmsg);

frame_t* evbuffer_read_frame(struct evbuffer* buf);
msg_t* evbuffer_read_msg(struct evbuffer* buf);
zmsg_t* evbuffer_read_zmsg(struct evbuffer* buf);

msg_t* to_msg(zmsg_t* svcmsg);
zmsg_t* to_zmsg(msg_t* zmsg);



#ifdef __cplusplus
}
#endif

#endif
