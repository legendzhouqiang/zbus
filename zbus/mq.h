#ifndef __MQ_H_
#define __MQ_H_

#include "platform.h" 
#include "list.h"
#include "hash.h"
#include "evzmsg.h"
#include <event2/bufferevent.h>

#ifdef __cplusplus
extern "C" {
#endif
 
#define MQ_ROLLER 0
#define MQ_MATCH  1
#define MQ_FILTER 2


typedef struct mq mq_t;
typedef struct sess sess_t;



/*
 * 消息队列
 * 所有操作算法复杂度O(1): 插入消息，弹出消息，指定Session弹出消息...
 * 消息队列模式：1) Roller, 消息订阅者Round-Robin负载收取
 *               2) Match，根据订阅者ID收取消息
 *               3) Filter, 根据订阅者指定主题收取消息
 */
struct mq {
	/* MQ标识 */
	char* name;
	/* 访问控制码 */
	char* token;
	
	/* 接收者消息队列集合，以接收者标识为键，对消息只有指向权 */
	hash_t* recv_msg_hash;
	/* 消息队列所属全部消息列表，对消息有所有权*/
	list_t* glob_msg_list;
	
	/* 订阅者映射，对Session只有指向权 */
	hash_t* recv_hash;
	/* 订阅者列表，对Session有所有权*/
	list_t* recv_list;
	
	/* 消息队列模式：Roller(0), Match(1), Filter(2)*/
	int mode; 
};

/* 
 * 产生默认Session标识，默认ID=Socket的FD
 * 出参：default_sessid，调用者提供空间
 */
void gen_default_sessid(char* default_sessid, struct bufferevent* bev);
/* 
 * 创建Session，sessid为NULL时，采用默认的ID, 对bev只有指向权
 */
sess_t* sess_new(char* sessid, struct bufferevent* bev);
void sess_destroy(sess_t** self_p); 


mq_t* mq_new(char* name, char* token, int mode);
void mq_destroy(mq_t** self_p);

/* 
 * 根据指定接收者标识获消息，recver_id=NULL则获取消息队列中的头部消息
 * 无消息返回NULL
 * 算法复杂度O(1)
 */
zmsg_t* mq_fetch_msg(mq_t* self, char* recver);
/* 
 * 消息入队列尾部，根据消息recver标识索引消息到receiver索引的消息队列尾部
 * 算法复杂度O(1)
 */
void  mq_push_msg(mq_t* self, zmsg_t* zmsg);

/* 
 * 入消息队列的订阅者队列，recver=NULL则使用默认ID
 * 算法复杂度O(1)
 */
void mq_put_recver(mq_t* self, char* recver, struct bufferevent* bev);

/* 
 * 根据recver标识删除订阅者，recver不能为NULL
 * 算法复杂度O(1)
 */
void mq_rem_recver(mq_t* self, char* recver); 
/* 
 * 根据订阅者标识（recver）获取订阅者，找不到返回NULL
 * 算法复杂度O(1)
 */
sess_t* mq_get_recver(mq_t* self, char* recver); 

/* 
 * 分发消息，根据消息的recver标识分发到消息队列的订阅者队列
 * 消息所有权释放 1）有满足的订阅者，消息发送出去
 *                2) 无满足的订阅者，消息归属订阅者队列
 */
void mq_dispatch_zmsg(mq_t* self, zmsg_t* zmsg);
/* 
 * 分发Session标识指定的所有消息
 * 分发到Sesion标识指定的Session
 */
void mq_dispatch_recver(mq_t* self, char* recver);


#ifdef __cplusplus
}
#endif

#endif
