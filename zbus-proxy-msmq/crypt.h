#ifndef CRYPT_H
#define CRYPT_H

#ifdef __cplusplus
extern "C" {
#endif

#include "msg.h"

#define DO_CRYPT "_do_crypt_"
msg_t* crypt_handler(msg_t* req);

#ifdef __cplusplus
}
#endif

#endif

