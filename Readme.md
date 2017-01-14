# Gist of zbus project: ZBUS = MQ + RPC 

 Do **NOT** use code of **master** in production, still alpha

##API
	MqConfig
		+ broker
		+ topic
		+ appid
		+ token
		+ consumeGroup
		+ consumeWindow
		+ consumeTimeout
		+ filterTag
	
	MqAdmin
		+ declareTopic()
		+ queryTopic()
		+ removeTopic()
	
	Producer
		+ publish(msg)
	
	Consumer
		+ take()
		+ route(msg)
	
	ConsumerServiceConfig : MqConfig
		+ messageHandler
		+ messagePrefetchCount
		+ parallelFactor
	
	ConsumerService
		+ onMessage(msgHandler)
		+ start()
		+ pause()
		+ close()
 	
	Broker
		+ selectForProducer(topic: String)
		+ selectForConsumer(topic: String)
		+ release(clients: MessageClient)
	
	ZbusBroker: Broker
		- SingleBroker: Broker
		- MultiBroker: Broker
		- TrackBroker: Broker
	JvmBroker: Broker

## Package
	
	io.zbus.mq.{Broker},{Producer},{Consumer},{Message}
	io.zbus.mq.broker.*
	io.zbus.mq.disk.*
	io.zbus.mq.server.*
	io.zbus.mq.tracker.*
	
	io.zbus.rpc.{Request},{Response},{RpcInvoker},{RpcProccessor},{RpcFactory}
	
	io.zbus.net -- net abstraction
	io.zbus.kit -- useful tools including pool

##MQ Model
	Topic   |||||||||||||||||||||| <-- Produce Write
	                   ^-------ConsumeGroup1              (reader group1)
	                       ^-------ConsumeGroup2          (reader group2)
	               ^-------ConsumeGroup3                  (reader group3)

Unified model for unicast, multicast, broadcast messaging style
		
## Protocol

* HTTP format compatible, but TCP based. 
* Control via HTTP header extension.

**Gist of HTTP Format**

	Request/Responose\r\n -- First line to distinguish between request and response message type, e.g. GET /(request), 200 OK(response)
	(Key: Value\r\n)*     -- lines for key-value pairs
	\r\n                  -- Separate Header and Body
	Body                  -- Body binary, length controlled by 'Content-Length: {number}' key-value in header


MqServer uses a HTTP header extension key called 'cmd' to distinguish job requested from clients.

**Commands Support**

	cmd=produce -- produce message(s) to MqServer
	cmd=consume -- consume message(s) from MqServer
	cmd=declare_topic -- create or update a topic with consume group details.
	cmd=query_topic -- query topic details
	cmd=remove_topic -- remove topic
	cmd=ping -- ping test, return server's time to response
	cmd=route -- route message back to a producer, designed for RPC

**Commond Headers**

	[R]cmd={string}
	[R]id={string, max:39}
	[R]topic={string}
	[O]appid={string}
	[O]token={string}

**cmd = produce**

	[O]tag={string, max:127}
	[O]ack={boolean}
	
	[O]body={binary}

**cmd = consume**

	[O]consume_group={string}
	[O]consume_window={number}

**cmd = declare_mq**

	[O]flag={integer} -- flag set to MQ
	
	[O]consume_group={string} -- default to MQ name, consume_group to create or update
	[O]consume_base_group={string} -- default to null, consume_group copy reader status from base_group
	[O]consume_start_offset={long} -- default to null, start_offset gets high priority over start_time
	[O]consume_start_msgid={string, max:39} -- default to null, extra field to locate start_offset
	[O]consume_start_time={long} -- default to null, located to first message with timestamp>start_time
	[O]consume_filter_tag={string, max:127} -- default to null, dot to define layers, such as abc.xyz, abc.*, abc.#, abc.#.xyz
	
	Priority: offset > time > base_group

**cmd = query_mq**

	[O]consume_group={string}

**cmd = remove_mq**

	[O]consume_group={string}

**cmd = route**

	[R]recver={string} -- route back message to a specified producer

**HTTP URL Access**

Url parse rule: 1) requestPath trimmed of / => cmd, 2)key-value extracted to override header if missing.

	/produce/?topic=MyTopic
	/consume/?topic=MyTopic&&consume_group=xxxxx
	/declare_mq/?topic=MyTopic
	/query_mq/?topic=MyTopic
	/remove_mq/?topic=MyTopic
	/route/?topic=MyTopic&&recver=xxxxx
	
	/ping
 
##RPC Protocol

	Request{
		+ module: String
		+ method: String
		+ args: String[]
	}

	Response{
		+ result: Object
		+ stackTrace: String
		+ error: Throwable
	}
 
 
##Monitor

	Topic:
	- Topic Name, Flag, ConsumeGroups, MQ Disk, LastActivity, InnerReplyQueue
	- Create, Pause, Resume, Remove Topic
	
	ConsumeGroup:
	- Remaining MessageCount, MessageSize
	- Pull Session List
	- Create Pause Resume, Remove ConsumeGroup
	
	Trace Message:
	- Latest Messge Passing Through
	- Search Message By Offset + MsgId
	- Search Message By TimeRange + MsgId
	
	Security:
	- Create/Update/Remove Appid + Token
	- Assign/Remove Appid + Token => MQ
	
	Extra:
	- Grey out node, Weight on node
 
##Tracker

	Topic Details: broker, topic, flag, consumeGroups, DiskInfo
	ConsumeGroup: name, activeConsumerCount, remainingMsgCount
	
	topic => broker list
	broker => topic list
	
	/pub
	/sub


##Security
AppId + Token


##Service Bus
- Java/.NET/C_C++/JS/Python api support
- Micro-Service oriented
- zbus-msmq
- zbus-kcxp
- zbus-webservice