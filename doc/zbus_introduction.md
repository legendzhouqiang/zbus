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
而依赖Broker对象而存在的Producer与Consumer一般可以看成是轻量级对象（Consumer也因为拥有链接需要关闭）



##zbus实现RPC服务


##zbus实现异构服务代理


##zbus高可用模式


##zbus性能测试数据


##zbus底层编程扩展

1. zbus网络编程模型
2. zbus协议说明


	
