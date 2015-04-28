# 轻量级消息队列、服务总线ZBUS之C#.NET客户端

##**ZBUS** 特性
[ZBUS消息队列、服务总线](http://git.oschina.net/rushmore/zbus "zbus") 

* **消息队列 -- 生产者消费者模式、发布订阅**
* **服务总线 -- 适配改造已有业务系统，使之具备跨平台与语言, RPC**
* **RPC -- 分布式远程方法调用，Java方法透明代理**
* **跨平台、多语言**
* **轻量级, 无依赖单个jar包**
* **高可用、高并发**



##**zbus-c#** 特性

* **无依赖**


##**zbus-c#** 示例

### 生产者

	RemotingClient client = new RemotingClient("127.0.0.1:15555");

    Producer producer = new Producer(client, "MyMQ", MessageMode.MQ);
            
    Message msg = new Message();
    msg.Topic = "qhee";
    msg.SetBody("hello world from C# {0}", DateTime.Now);
    msg = producer.Send(msg, 10000); 


### 消费者

	RemotingClient client = new RemotingClient("127.0.0.1:15555");
    Consumer csm = new Consumer(client, "MyMQ"); 
    while (true)
    {
        Message msg = csm.Recv(10000);
        if (msg == null) continue;
        Console.WriteLine(msg);
    } 
	    


##**TODO** 
增加Service/Broker/HaBroker高可用支持