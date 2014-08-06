# ZBUS--服务总线/消息队列【轻量级】 #

> ###**ZBUS**解决问题领域###

* **消息队列 -- 生产者消费者模式、发布订阅、远程方法调用RPC**
* **跨平台、多语言**
* **极度轻量级**
* **追求服务高可用、高并发**
* **企业SOA服务实施轻量级核心组件**

> ###**ZBUS**设计实现理念###

* ZBUS追求极度轻量级，<200K 发行jar包（从早期的基于ZeroMQ C实现演化为JAVA NIO实现），不依赖任何其他包，
* 高度可扩展（异步通讯NIO，Remoting，日志、JSON协议格式等等都可以动态更换扩展）
* 兼容HTTP协议标准（协议本身**兼容**，原生支持不是适配，浏览器HTTP可以直接与zbus互动）
* 丰富API轻量级接入： C/C++,C#, JAVA, Python，Node.JS等主流平台不断丰富


## ZBUS总线启动 ##

1. 通过发行jar包启动，进入bin目录下选择zbus.sh或者zbus.bat直接执行
2. 通过源码ZbusServer.java启动

总线默认占用**15555**端口， [http://localhost:15555](http://localhost:15555 "默认监控地址") 可以直接进入监控，注意zbus因为原生兼容HTTP协议所以监控与消息队列使用同一个端口

**高可用模式启动总线**
分别启动ZbusServer与TrackServer，无顺序之分，默认ZbusServer占用15555端口，TrackServer占用16666端口。高可用部分后续专门介绍。

## ZBUS API通用模式 ##

>ZBUS把物理连接与消息模式分离，上层消息模式共享底层物理连接，所以一般分两部分完成消息模式客户端的创建

1. 创建到zbus通讯链接--**RemotingClient**
2. 通过RemotingClient创建**Producer**、**Consumer**、**Rpc**等消息模式对象


## ZBUS 示例 ##

### *1* 生产者Producer###
**生产者Python示例**
    
    from zbus import Message, RemotingClient, Producer
    #1）创建通讯链接
    client = RemotingClient(broker='127.0.0.1:15555') 
    #2）创建生产对象，指定队列
    p = Producer(client=client, mq='MyMQ')           
    msg = Message()
    msg.set_body('hello world') 
    #3）生产消息
    p.send(msg) 
    
    client.close() 
    
**生产者Java示例**
    
    package org.zbus;
    
    import org.remoting.Message;
    import org.remoting.RemotingClient;
    import org.remoting.ticket.ResultCallback;
    import org.zbus.client.Producer; 
    
    public class ProducerWithClient {
    	public static void main(String[] args) throws Exception {   
    		//1) 创建到ZbusServer的链接
    		final RemotingClient client = new RemotingClient("127.0.0.1:15555"); 
    		
    		//2) 包装为生产者，client生命周期不受Producer控制，因此Producer是个轻量级对象
    		Producer producer = new Producer(client, "MyMQ");  
    		Message msg = new Message();  
    		msg.setBody("hello world"); 
    		producer.send(msg, new ResultCallback() { 
    			@Override
    			public void onCompleted(Message result) {  
    				System.out.println(result); 
    			}
    		});  
            client.close();
    	} 
    }
**生产者C/C++示例**
    
	#include "zbus.h"
    int main_producer(int argc, char* argv[]){
    	//1)创建通讯链接
    	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000);
    	//2)创建生产对象，指定队列
    	producer_t* p = producer_new(client, "MyMQ", MODE_MQ);
    	
    	msg_t* msg, *res = NULL;
    	int rc;
    	
    	msg = msg_new();
    	msg_set_body(msg, "hello world");
    	//3)生产消息	
    	rc = producer_send(p, msg, &res, 10000);
    	if(rc>=0 && res){
    		msg_print(res);
    		msg_destroy(&res);
    	} 
    	
    	getchar();
    	producer_destroy(&p);
    	rclient_destroy(&client);
    	return 0;
    }


### *2* 消费者Consumer###

**消费者Python示例**

    from zbus import RemotingClient, Consumer
    client = RemotingClient(broker = '127.0.0.1:15555')
    consumer = Consumer(client=client,mq='MyMQ')  
    while True:
    	msg = consumer.recv()
    	if msg is None: continue
    	print msg

**消费者Java示例**

    package org.zbus;
    
    import java.io.IOException;
    
    import org.remoting.Message;
    import org.remoting.RemotingClient;
    import org.zbus.client.Consumer; 
    
    public class ConsumerWithClient { 
    	public static void main(String[] args) throws IOException{  
    		//1) 创建到ZbusServer的链接
    		final RemotingClient client = new RemotingClient("127.0.0.1", 15555);
    		//2) 包装为消费者，client生命周期不受Consumer控制，因此Consumer是个轻量级对象
    		Consumer consumer = new Consumer(client, "MyMQ");
    		while(true){
    			Message msg = consumer.recv(10000);
    			if(msg == null) continue; 
    			System.out.println(msg); 
    		} 
    	} 
    }
    
**消费者C/C++示例**
    
    #include "zbus.h" 
    int main(int argc, char* argv[]){
    	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000);
    	consumer_t* consumer = consumer_new(client, "MyMQ", MODE_MQ);
    	msg_t*res = NULL;
    	int rc;
    	while(1){
    		rc = consumer_recv(consumer, &res, 10000);
    		if(rc<0) continue; 
    		if(rc>=0 && res){
    			msg_print(res); 
    			msg_destroy(&res);
    		}
    	}
    	getchar();
    	consumer_destroy(&consumer);
    	rclient_destroy(&client);
    	return 0;
    }


###PubSub发布订阅###

**Pub发布消息 Python示例**


    from zbus import Message, RemotingClient, Producer, MessageMode
    #整体与生产者几乎类似，除了指定消息模式为PubSub
    client = RemotingClient(broker='127.0.0.1:15555')
    p = Producer(client=client, 
      				mq='MySub',
      				mode=MessageMode.PubSub) #指定消息模式为发布订阅
     
    msg = Message()  
    msg.set_topic('qhee')  #指定消息的主题
    msg.set_body('hello world') 
    print p.send(msg)
    
    client.close()


**Sub订阅消息 Python示例**
    
    from zbus import RemotingClient, Consumer, MessageMode
    
    client = RemotingClient(broker = '127.0.0.1:15555')
    
    consumer = Consumer(client=client, 
    						mq='MySub', 
    						mode=MessageMode.PubSub)#指定消息模式
    consumer.topic = 'qhee,xmee' #指定感兴趣的消息主题，用','分割不同主题
    
    while True:
	    msg = consumer.recv()
	    if msg is None: continue
	    print msg
     
    
**Pub发布消息 JAVA示例**

    package org.zbus;
    
    import org.remoting.Message;
    import org.remoting.RemotingClient;
    import org.remoting.ticket.ResultCallback;
    import org.zbus.client.Producer;
    import org.zbus.common.MessageMode;
    
    
    public class PubWithClient {
    
    	public static void main(String[] args) throws Exception {  
    		final RemotingClient client = new RemotingClient("127.0.0.1", 15555); 
    		//指定消息模式为发布订阅
    		Producer producer = new Producer(client, "MySub", MessageMode.PubSub); 
    		Message msg = new Message();  
    		msg.setTopic("qhee"); //设定消息主题
    		msg.setBody("hello world"); 
    		producer.send(msg, new ResultCallback() { 
    			@Override
    			public void onCompleted(Message result) {  
    				System.out.println(result); 
    			}
    		}); 
    	}  
    }
    

**Sub订阅消息 JAVA示例**


    package org.zbus;
    
    import org.remoting.Message;
    import org.remoting.RemotingClient;
    import org.zbus.client.Consumer;
    import org.zbus.common.MessageMode;
    
    public class SubWithClient {
    
    	public static void main(String[] args) throws Exception {  
    		
    		final RemotingClient client = new RemotingClient("127.0.0.1:15555");	
    		final Consumer consumer = new Consumer(client, "MySub", MessageMode.PubSub);   
    		consumer.setTopic("qhee,xmee");  
    		while(true){
    			Message msg = consumer.recv(10000); 
    			if(msg == null) continue;
    			System.out.println(msg);
    		}
    	}
    
    }
    


###RPC远程调用###

**RPC Python示例，服务实现**
    
    from zbus import RpcService, RpcServiceConfig, ServiceHandler, Message
    import time
    #服务示例，直接返回200 OK，服务时间
    class MyServiceHandler(ServiceHandler):
	    def handle_request(self, msg): 
		    print msg
		    res = Message()
		    res.set_status('200')
		    res.set_body('hello server@%s'%time.time())
		    return res
    
    handler = MyServiceHandler() 
    
    #配置信
    config = RpcServiceConfig()
    config.broker = '127.0.0.1:15555' #总线地址
    config.service_name = 'MyRpc' #服务队列名称
    config.thread_count = 1 #线程数配置
    config.service_andler = handler
    
    svc = RpcService(config)
    svc.start()
    svc.join()
    
**RPC Python示例，服务调用** 
    
    from zbus import RemotingClient, Rpc, Message
    client = RemotingClient(broker='127.0.0.1:15555')
    rpc = Rpc(client=client, mq='MyRpc') 
    
    msg = Message()
    msg.set_body('hello?') #构造消息
    print rpc.invoke(msg)  #直接调用请求
    
    client.close()
    
    
###JsonRpc远程调用###

**JsonRpc Python示例，服务实现**
    
    
    from zbus import RpcService, RpcServiceConfig, JsonServiceHandler, Remote
    
    class MyService(object): 
    
	    @Remote()
	    def echo(self, ping):
	    	return ping
	    
	    @Remote()
	    def save(self, user):
	    	print user
	    	return 'OK'
	    
	    @Remote('user')
	    	def user(self, username):
	   		return {'Name': username, 'Addr': u'中文'}
	    
	    @Remote()
	    def plus(self, a, b):
	    	print 'plus(%s,%s)'%(a, b)
	    	return a + b 
     
    handler = JsonServiceHandler()
    handler.add_module('ServiceInterface', MyService())
    
    config = RpcServiceConfig()
    config.broker = '127.0.0.1:15555'
    config.service_name = 'MyJsonRpc'
    config.service_andler = handler
    config.thread_count = 1
    
    svc = RpcService(config)
    svc.start()
    svc.join()


**JsonRpc Python示例，服务调用**
    
    from zbus import RemotingClient,JsonRpc 
    
    client = RemotingClient(broker='127.0.0.1:15555')
    
    rpc = JsonRpc(client=client, 
      				mq='MyJsonRpc', 
      				module='ServiceInterface')
    
    print rpc.plus(1,2)

     

### **TODO**
* 1) 增加C、Python平台异步操作API
* 2) 增加Node.JS平台接入


