#ifndef __ZBUSCONN_H__
#define __ZBUSCONN_H__

#include "remoting.h"  
#include "json.h"

#ifdef __cplusplus
extern "C" {
#endif
 
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
