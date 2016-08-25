# ZBUS C# SDK

##**ZBUS = MQ + RPC** 
[ZBUS=MQ+RPC](http://git.oschina.net/rushmore/zbus "zbus") 


##**ZBUS-C#** Features

* **No Dependency**
* **Support.NET 2.0+**


##**ZBUS-C#** Examples

### Requirements
Before you run any following example, you have to start zbus first, since the application is broker 
based. Please see the guide in [ZBUS=MQ+RPC](http://git.oschina.net/rushmore/zbus "zbus") project to configure zbus.

### Consumer Example

    //1) create a Broker
    IBroker broker = new SingleBroker(); //using BrokerConfig to change default

    //2) create a consumer with the broker and MQ name in ZBUS
    Consumer c = new Consumer(broker, "MyMQ");
    c.ConsumeTimeout = 3000;

	//3) define your own message handler
    c.OnMessage(new MyHandler()); 
    //4) start the consumer thread
    c.Start();
    
    //c.Stop(); 
    //broker.Dispose();
    
    
### Producer Example

	//1) create a broker to zbus(using BrokerConfig to change default)
    IBroker broker = new SingleBroker();
    //2) create a producer with broker and MQ name in ZBUS
    Producer producer = new Producer(broker, "MyMQ");
    //3) create MQ if needed
    producer.CreateMQ(); 

    Message msg = new Message(); 
    msg.SetBody("hello world from C# {0}", DateTime.Now);
    msg = producer.Send(msg, 10000); //timeout for waiting reply from zbus

    //4) dispose the broker
    broker.Dispose(); 

### RPC Service Example

	IBroker broker = new SingleBroker(); //using default configuration

    RpcProcessor processor = new RpcProcessor(new MyService());// business domain service
    
    ServiceConfig config = new ServiceConfig(broker);
    config.Mq = "MyRpc"; //Service entry in zbus as a MQ
    config.MessageProcessor = processor;
    config.ConsumerCount = 32;
 
    Service service = new Service(config);
    service.Start();


### RPC Client Example
    //1) create broker
    IBroker broker = new SingleBroker();

    RpcConfig config = new RpcConfig();
    config.Mq = "MyRpc"; //MQ entry in zbus serving for the RPC
    config.Broker = broker;
    //2) create Rpc object, 
    Rpc rpc = new Rpc(config);
    //3) invoke a method with params
    object res = rpc.Invoke("getString", "test");  
    
    broker.Dispose(); 
    Console.ReadKey();

 