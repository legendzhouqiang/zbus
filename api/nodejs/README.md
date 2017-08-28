# 轻量级消息队列、服务总线ZBUS之Node.JS客户端

##**ZBUS** 特性
[ZBUS消息队列、服务总线](http://git.oschina.net/rushmore/zbus "zbus") 

* **消息队列 -- 生产者消费者模式、发布订阅**
* **服务总线 -- 适配改造已有业务系统，使之具备跨平台与语言, RPC**
* **RPC -- 分布式远程方法调用，Java方法透明代理**
* **跨平台、多语言**
* **轻量级, 无依赖单个jar包**
* **高可用、高并发**



##**zbus-nodejs** 特性

* **单个js文件**


##**zbus-node.js** 示例

### 生产者

	var zbus = require("../zbus");
	
	var Message = zbus.Message;
	var MessageClient = zbus.MessageClient;
	var Producer = zbus.Producer; 
	
	var client = new MessageClient("127.0.0.1:15555");
	
	
	client.connect(function(){
	    var producer = new Producer(client, "MyMQ");
	    var msg = new Message();
	    msg.setBody("hello world from node.js");
	    
	    producer.send(msg, function(res){
	        console.log(res.toString());
	    });
	});
		


### 消费者

	var zbus = require("../zbus");
	var Message = zbus.Message;
	var MessageClient = zbus.MessageClient;
	var Consumer = zbus.Consumer;
	
	
	var client = new MessageClient("127.0.0.1:15555");
	client.connect(function(){
	    var consumer = new Consumer(client, "MyMQ");
	    consumer.recv(function(msg){
	        console.log(msg.toString());
	    });
	});


##**TODO** 
增加Broker/HaBroker抽象隔离RemotingClient