# zbus消息协议 

## 协议概览 
zbus协议继承于HTTP协议格式，主体采用HTTP头部协议扩展完成,HTTP协议由HTTP头部和HTTP包体组成，
zbus协议在HTTP头部KV值做了扩展，支持浏览器方式直接访问，但是zbus链接机制采用保持长连接方式。
原则上, 编写客户端SDK只需要遵循下面zbus协议扩展即可。

zbus扩展头部主要是完成

*   消息命令
*   zbus消息队列寻址
*	异步消息匹配
*	安全控制

扩展头部Key-Value解释

###1. 消息命令
命令标识，决定Broker（zbusServer|TrackServer)的处理 

cmd: produce | consume | request | heartbeat | admin(默认值)

###2. 消息队列寻址

mq: 消息目标队列

mq_reply: 消息回复队列

###3. 异步消息匹配
msgid: 消息唯一UUID

msgid_raw: 原始消息唯一UUID, 消息消费路由后ID发生变化，该字段保留最原始的消息ID

###4. 安全控制
token: 访问控制码，不填默认空

###5. 其他可扩展
broker: 消息经过Broker的地址

topic: 消息主题，发布订阅时使用

ack: 是否需要对当前消息ACK，不填默认true

encoding: 消息体的编码格式

sub_cmd: 管理命令的二级命令


###6 HTTP头部第一行，zbus协议保持一致理解

请求：GET|POST URI

应答：200 OK 

URI做扩展Key-Value的字符串理解


## 协议细节 
 
### 生产者Produce

请求格式

*	[必填]cmd: produce 
*	[必填]mq: 目标队列 
*	[可选*]msgid: 消息UUID， 需要ACK时[必填]
*	[可选]mq_reply: 回复队列，默认为请求UUID。 需要应答的时候由mq_reply + msgid路由返回
*	[可选]topic: 发布订阅时发布消息的主题
*	[可选]token: 访问控制码
*	[可选]HTTP消息体 承载业务数据

应答格式（在启用ack的时候才有应答）

*	[可选]msgid: 消息UUID=请求消息UUID，客户端匹配使用


### 消费者Consume



请求格式

*	[必填]cmd: consume 
*	[必填]mq: 目标队列 
*	[必填]msgid: 消息UUID 
*	[可选]topic: 发布订阅时订阅感兴趣消息的主题
*	[可选]token: 访问控制码 

应答格式

*	[必填]msgid: 消息UUID，为了匹配消费请求
*	[必填]broker: 消息路由经历的Broker地址
*	[可选]mq_reply: 回复队列, 需要反馈结果的Consumer利用mq_reply指定目标消息队列
*	[可选]msgid_raw: 原始消息UUID，需要反馈结果的Consumer利用msgid_raw指定回复消息ID
*	[可选]HTTP消息体 承载业务数据



### 服务请求Request


请求格式

*	[必填]cmd: request 
*	[必填]mq: 目标队列 
*	[必填]msgid: 消息UUID
*	[必填]mq_reply: 回复队列，不制定的情况下默认为当前发送者的UUID 
*	[可选]token: 访问控制码
*	[可选]HTTP消息体 承载业务数据

应答格式（在启用ack的时候才有应答）

*	[必填]msgid: 消息UUID=请求消息UUID，客户端匹配使用
*	[可选]HTTP消息体 承载业务数据


### 监控管理


请求格式

*	[可选]cmd: admin，不填写默认为admin 
*	[可选]sub_cmd: create_mq
*	[可选*]msgid: 消息UUID, 客户端需要消息匹配时需指定
*	[可选]token: 访问控制码
*	[可选]HTTP消息体 承载业务数据

应答格式

*	[可选*]msgid: 消息UUID=请求消息UUID，客户端匹配使用
*	[可选]HTTP消息体 承载业务数据


### URI格式


URI = /   
 
监控首页 = /?cmd=admin&&method=index


URI = /MyMQ 

第一个?之前理解为消息队列 mq=MyMQ


*	/MyMQ?cmd=produce&&msgid=aed14-2343-1dea0-32&&body=xxxyyyzzz
*	/MyMQ?cmd=consume&&msgid=aed14-2343-1dea0-32
*	/MyMQ?cmd=request&&msgid=aed14-2343-1dea0-32

第一个?之后理解为Key-Value, URI的KV优先级低于头部扩展

