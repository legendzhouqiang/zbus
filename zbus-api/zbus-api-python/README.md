# 轻量级消息队列、服务总线ZBUS之Python客户端

##**ZBUS** 特性
[ZBUS消息队列、服务总线](http://git.oschina.net/rushmore/zbus "zbus") 

* **消息队列 -- 生产者消费者模式、发布订阅**
* **服务总线 -- 适配改造已有业务系统，使之具备跨平台与语言, RPC**
* **RPC -- 分布式远程方法调用，Java方法透明代理**
* **跨平台、多语言**
* **轻量级, 无依赖单个jar包**
* **高可用、高并发**



##**zbus-python** 特性

* **单个Py文件**


##**zbus-python** 示例

### 生产者

	from zbus import SingleBroker, Producer, Message
	broker = SingleBroker(host = 'localhost', port = 15555)
	
	#Producer是轻量级对象,不需要关闭
	p = Producer(broker=broker, mq='MyMQ') 
	
	msg = Message()    
	msg.set_body('hello world') 
	print p.send(msg)
	
	
	broker.destroy()


### 消费者
	from zbus import SingleBroker, Consumer
	broker = SingleBroker(host = 'localhost', port = 15555)
	
	consumer = Consumer(broker=broker,mq='MyMQ') 
	
	 
	while True:
	    msg = consumer.recv()
	    if msg is None: continue
	    print msg
	    


##**TODO** 
增加HaBroker高可用支持