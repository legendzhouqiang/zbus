#ifndef __REMOTING_H__
#define __REMOTING_H__

#include "platform.h"  
#include "msg.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct rclient rclient_t;
ZBOX_EXPORT rclient_t* rclient_new(char* broker);
ZBOX_EXPORT void       rclient_destroy(rclient_t** self_p);
ZBOX_EXPORT rclient_t* rclient_connect(char* broker, int auto_reconnect_millis);
ZBOX_EXPORT bool       rclient_reconnect(rclient_t* self, int reconnect_msecs); 

ZBOX_EXPORT int  rclient_probe(rclient_t* self);
ZBOX_EXPORT int  rclient_send(rclient_t* self, msg_t* msg);
ZBOX_EXPORT int  rclient_recv(rclient_t* self, msg_t** msg_p, int timeout);
ZBOX_EXPORT int  rclient_invoke(rclient_t* self, msg_t* req, msg_t** res_p, int timeout);


#ifdef __cplusplus
}
#endif

#endif
