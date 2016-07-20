# 轻量级消息队列、服务总线ZBUS之C/C++客户端

##**ZBUS** 特性
[ZBUS消息队列、服务总线](http://git.oschina.net/rushmore/zbus "zbus") 

* **消息队列 -- 生产者消费者模式、发布订阅**
* **服务总线 -- 适配改造已有业务系统，使之具备跨平台与语言, RPC**
* **RPC -- 分布式远程方法调用，Java方法透明代理**
* **跨平台、多语言**
* **轻量级, 无依赖单个jar包**
* **高可用、高并发**



##**zbus-c/c++** 特性

* **跨平台**


##**zbus-c/c++** 示例

### 生产者

	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000); 
	//create Producer by MQ and RemotingClient
	producer_t* p = producer_new(client, "MyMQ", MODE_MQ);

	msg_t* msg, *res = NULL;
	int rc;

	msg = msg_new();
	msg_set_body(msg, "hello world"); 
	rc = producer_send(p, msg, &res, 10000);
	if(rc>=0 && res){
		msg_print(res);
		msg_destroy(&res);
	} 

	getchar();
	producer_destroy(&p);
	rclient_destroy(&client);


### 消费者
	rclient_t* client = rclient_connect("127.0.0.1:15555", 10000);
	consumer_t* consumer = consumer_new(client, "MyMQ2", MODE_MQ);
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
	    


##**TODO** 
改成Broker模式隔离对rclient的依赖
