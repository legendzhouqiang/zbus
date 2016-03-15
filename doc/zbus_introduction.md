# ZBUS = MQ + RPC

##zbus解决的问题域
1. 消息队列 -- 应用解耦
2. 分布式RPC -- 远程方法调用
3. 异构服务代理 -- 跨平台语言RPC改造，实现DMZ服务总线架构

##zbus目前不解决
1. 分布式事务

##zbus特点
1. 极其轻量级--单个Jar包无依赖 ~300K （可个性化适配各类log包，commons-pool包）
2. 亿级消息堆积能力、支持HA高可用
3. 丰富的API--JAVA/C/C++/C#/Python/Node.JS多语言接入 
4. 兼容扩展HTTP协议接入（方便新增客户端SDK）

##QQ讨论组：467741880

##启动zbus的几种方法
zbus的角色是中间消息服务（Broker），默认分布式运行（当然也可以嵌入式单进程运作）

1. 通过脚本直接运行 zbus-dist发行目录下windows下对应zbus.bat, linux/mac 对应zbus.sh
   运行脚本可以JVM参数优化，MQ存储路径等配置，如果运行发生错误，重点检查 （1）是否正确配置JVM （2）端口是否占用
2. 嵌入式直接 new MqServer 启动

	MqServerConfig config = new MqServerConfig();   
	config.serverPort = 15555;  
	config.storePath = "./store";  
	final MqServer server = new MqServer(config);  
	server.start();  
	
启动后zbus可以通过浏览器直接访问zbus启动服务器15555端口的监控服务


##zbus实现消息队列

消息队列是zbus的最基础服务，MQ参与角色分为三大类

1. Broker中间消息服务器
2. Producer生产者
3. Consumer消费者

Producer ==> Broker ==> Consumer

逻辑上解耦分离
1. 生产者只需要知道Broker的存在，负责生产消息到Broker，不需要关心消费者的行为
2. 消费者也只需要知道Broker的存在，负责消费处理Broker上某个MQ队列的消息，不需要关心生产者的行为

不同的Broker实现在细节上会有些不同，但是在MQ逻辑解耦上基本保持一致，下面细节全部是以zbus特定定义展开

zbus与客户端（生产者与消费者）之间通讯的消息（org.zbus.net.http.Message）为了扩展性采用了【扩展HTTP】消息格式。
zbus的消息逻辑组织是以MQ标识来分组消息，MQ标识在zbus中就是MQ名字，Message对象中可以直接指定。
物理上zbus把同一个下MQ标识下的消息按照FIFO队列的模式在磁盘中存储，队列长度受限于磁盘大小，与内存无关。

编程模型上，分两个视图，生产者与消费者两个视图展开

1. 生产者视图
2. 消费者视图

生产者与消费者在编程模型上都需要首先产生一个Broker，Broker是对zbus本身的抽象，为了达到编程模型的一致，Broker可以是
单机版本的SingleBroker，也可以是高可用版本的HaBroker，甚至可以是不经过网络的本地化JvmBroker，这些类型的Broker都是不同的实现，编程模型上不关心，具体根据实际运行环境而定，为了更加方便配置，ZbusBroker实现了上述几种不同的Broker实现的代理包装，根据具体Broker地址来决定最终的版本。

例如

	Broker broker = new ZbusBroker("127.0.0.1:15555"); //SingleBroker
	Broker broker = new ZbusBroker("127.0.0.1:16666;127.0.0.1:16667"); //HaBroker
	Broker broker = new ZbusBroker("jvm"); //JvmBroker

Broker内部核心实现了：
1. 连接池管理
2. 同步异步API

所以Broker在JAVA中可以理解为类似JDBC连接池一样的重对象，应该共享使用，大部分场景应该是Application生命周期。
而依赖Broker对象而存在的Producer与Consumer一般可以看成是轻量级对象（Consumer因为拥有链接需要关闭）


**生产消息**
		
	//Producer是轻量级对象可以随意创建不用释放 
	Producer producer = new Producer(broker, "MyMQ");
	producer.createMQ();//确定为创建消息队列需要显示调用

	Message msg = new Message();
	msg.setBody("hello world");  //消息体底层是byte[]
	msg = producer.sendSync(msg);

**消费消息**

	Consumer consumer = new Consumer(broker, "MyMQ");  
	consumer.start(new ConsumerHandler() { 
		@Override
		public void handle(Message msg, Consumer consumer) throws IOException { 
			//消息回调处理
			System.out.println(msg);
		}
	}); 
	//可控的范围内需要关闭consumer（内部拥有了物理连接）

生产者可以异步发送消息，直接调用producer.sendAsync()，具体请参考examples中相关示例

消费者可以使用更底层的API控制怎么取消息，直接调用consumer.take()从zbus上取回消息
    
从上面的API来看，使用非常简单，连接池管理，同步异步处理、高可用等相关主题全部留给了Broker抽象本身



##zbus实现RPC服务

MQ消息队列用于解耦应用之间的依赖关系，一般认为MQ是从更广泛的分布式RPC中演变而来的：在RPC场景下，如果某个远程方法调用耗时过长，调用方不希望blocking等待，除了异步处理之外，更加常见的改造方式是采用消息队列解耦调用方与服务方。

RPC的场景更加常见，RPC需要解决异构环境跨语言的调用问题，有非常多的解决方案，综合看都是折中方案，zbus也属其一。


RPC从数据通讯角度来看可以简单理解为：


	分布式调用方A --->命令打包(method+params) ---> 网络传输 --->  分布式式服务方B 命令解包（method+params）
	       ^                                                                            | 
    	   |                                                                            v
    	   |<---结果解包(result/ error)<------- 网络传输 <----  结果打包(result/ error) <---调用本地方法



异构环境下RPC方案需要解决的问题包括以下核心问题

	1. 跨语言，多语言平台下的消息通讯格式选择问题
	2. 服务端伺服问题，高性能处理模型
	3. 分布式负载均衡问题

WebService采用HTTP协议负载，SOAP跨语言描述对象解决问题1

Windows WCF采用抽象统一WebService和私有序列化高效传输解决问题1

在服务端处理模型与分布式负载均衡方面并不多体现，这里不讨论WebService，WCF或者某些私有的RPC方案的优劣之分，工程优化过程中出现了诸如Thrift，dubbo等等RPC框架，折射出来是的对已有的RPC方案中折中的不满。


针对问题1，zbus的RPC采用的是JSON数据根式封装跨语言平台协议，特点是简单明了，协议应用广泛（zbus设计上可以替换JSON）

针对问题2、问题3，zbus默认采用两套模式，MQ-RPC与DirectRPC， MQ-RPC基于MQ消息队列中间，DirectRPC则不通过MQ直连模式

zbus的RPC方案除了解决上面三个问题之外，还有两个重要的工程目标：

	4.极其轻量简单方便二次开发
	5.RPC业务本身与zbus解耦（方便直接替换掉zbus）


zbus的RPC设计非常简单，模型上对请求和应答做了基本的抽象

	public static class Request{ 
		private String module = ""; //模块标识
		private String method;      //远程方法
		private Object[] params;    //参数列表
		private String[] paramTypes;
		private String encoding = "UTF-8";
	}
	
	public static class Response {  
		private Object result;  
		private Throwable error;
		private String stackTrace; //异常时候一定保证stackTrace设定，判断的逻辑以此为依据
		private String encoding = "UTF-8";
	}

非常直观的抽象设计，就是对method+params 与 结果result/error 的JAVA表达而已。

RpcCodec的一个JSON实现JsonRpcCodec完成将上述对象序列化成JSON格式放入到HTTP消息体中在网络上传输

**RPC调用方**

RpcInvoker API核心
	
	public class RpcInvoker{ 
		public Response invokeSync(Request request){
			.....
		} 
	}
完成将上述请求序列化并发送至网络，等待结果返回，序列化回result/error。

RpcInvoker同时适配MQ-RPC与DirectRPC，只需要给RpcInvoker指定不同的底层消息MessageInvoker，比如
1. MqInvoker通过MQ做RPC
2. HaInvoker高可用直连RPC
3. MessageClient简单点对点





为了解决问题5，使得zbus在RPC业务解耦，zbus增加了动态代理类

RpcFactory API完成业务interface经过zbus的RPC动态代理类实现

	public class RpcFactory { 	
		public <T> T getService(Class<T> api) throws Exception{
			....
		}
	}

通过RpcFactory则完成了业务代码与zbus的解耦（通过spring等IOC容器更加彻底的把zbus完全隔离掉）




**RPC服务方**


##zbus实现异构服务代理



##zbus高可用模式


##zbus性能测试数据


##zbus底层编程扩展

1. zbus网络编程模型
2. zbus协议说明


	
