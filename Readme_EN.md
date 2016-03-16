# ZBUS -- Light-weighted MQ and RPC broker


##ZBUS = MQ + RPC 


* **Messaging Queue/PubSub**
* **Remote Procedure Call(RPC)**
* **Billions of message persistence capability** 
* **Support High availability(HA)**
* **Single jar distribution around 300K** 
* **HTTP compatible--Extends HTTP headers**
* **Multiple platform API--JAVA/C/C++/C#/Python/Node.JS etc.** 

## QQ Discussion: 467741880


## ZBUS 启动与监控 

zbus-dist选择zbus.sh或者zbus.bat直接执行

![简单监控](http://git.oschina.net/uploads/images/2015/0818/132153_425b58e9_7458.png)

总线默认占用 **15555** 端口， [http://localhost:15555](http://localhost:15555 "默认监控地址") 可以直接进入监控，注意zbus因为原生兼容HTTP协议所以监控与消息队列使用同一个端口


## ZBUS 角色概要

![zbus-arch](http://git.oschina.net/uploads/images/2015/0818/145645_0a1651bf_7458.png)


## ZBUS 消息通讯基础（NET模块）

ZBUS项目不依赖其他第三方库，消息通讯基于NIO完成（NET子项目）。NET包对NIO做了简洁的封装，相对Netty而言，学习成本低几个量级，模型简单，但不失扩展性。


![znet-arch](http://git.oschina.net/uploads/images/2015/0818/151248_bde11d15_7458.png)

框架结构保持 **Dispatcher + N SelectorThread + IoAdaptor**

**Dispatcher** 负责管理N个Selector线程

**SelectorThread** 负责NIO读写事件分发

**IoAdaptor** 个性化读写事件

基于NET的服务器程序基本只要关心IoAdaptor的个性化，比如ZBUS入口就是MqAdaptor


## ZBUS API

* [Java API](http://git.oschina.net/rushmore/zbus "zbus") 
* [C/C++ API](http://git.oschina.net/rushmore/zbus-api-c "zbus-api-c") 
* [Python API](http://git.oschina.net/rushmore/zbus-api-python "zbus-api-python") 
* [C# API](http://git.oschina.net/rushmore/zbus-api-csharp "zbus-api-csharp") 
* [Node.JS API](http://git.oschina.net/rushmore/zbus-api-nodejs "zbus-api-nodejs") 

## ZBUS PROXY

* [微软MSMQ|国信交易调度](http://git.oschina.net/rushmore/zbus-proxy-msmq "zbus-proxy-msmq") 
* [金证KCXP](http://git.oschina.net/rushmore/zbus-proxy-kcxp "zbus-proxy-kcxp") 
* [国信TC](http://git.oschina.net/rushmore/zbus-proxy-tc "zbus-proxy-tc") 
* [桥接JAVA客户端](http://git.oschina.net/rushmore/zbus-proxy-java "zbus-proxy-java") 
* [国泰君安GTA](http://git.oschina.net/rushmore/zbus-proxy-gta "zbus-proxy-gta")


## ZBUS 示例

### Java Maven 依赖

	<dependency>
		<groupId>org.zbus</groupId>
		<artifactId>zbus</artifactId>
		<version>6.2.9</version>
	</dependency>

### 生产者


	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); // 如果已经确定存在，不需要创建

		//创建消息，消息体可以是任意binary，应用协议交给使用者
		Message msg = new Message();
		msg.setBody("hello world");
		producer.sendSync(msg);  
		
		broker.close();
	}


### 消费者

	public static void main(String[] args) throws Exception{  
		//创建Broker代表
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);
		
		MqConfig config = new MqConfig(); 
		config.setBroker(broker);
		config.setMq("MyMQ");
		
		//创建消费者
		@SuppressWarnings("resource")
		Consumer c = new Consumer(config);  
		
		c.onMessage(new MessageHandler() { 
			@Override
			public void handle(Message msg, Session sess) throws IOException {
				System.out.println(msg);
			}
		});

		//启动消费线程
		c.start();   
		
	}  

 
### RPC动态代理【各类复杂类型】

参考源码test目下的rpc部分

		//1)创建Broker代表（可用高可用替代）
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		 
		//2)创建基于MQ的Invoker以及Rpc工厂，指定RPC采用的MQ为MyRpc
		MqInvoker invoker = new MqInvoker(broker, "MyRpc"); 
		RpcFactory factory = new RpcFactory(invoker); 
		
		//3) 动态代理出实现类
		Interface hello = factory.getService(Interface.class);
		
		test(hello);  
		
		broker.close();


 
 
### Spring集成--服务端(RPC示例)

**无任何代码侵入使得你已有的业务接口接入到zbus，获得跨平台和多语言支持**

	<!-- 暴露的的接口实现示例 -->
	<bean id="interface" class="org.zbus.rpc.biz.InterfaceImpl"></bean>
	
	<bean id="serviceProcessor" class="org.zbus.rpc.RpcProcessor">
		<constructor-arg>
			<list>
				<!-- 放入你需要的暴露的的接口 -->
				<ref bean="interface"/>
			</list>
		</constructor-arg>
	</bean>
	 
	<bean id="broker" class="org.zbus.broker.SingleBroker">
		<constructor-arg>
			<bean class="org.zbus.broker.BrokerConfig">
				<property name="serverAddress" value="127.0.0.1:15555" />
				<property name="maxTotal" value="20"/>
				<!-- 这里可以增加连接池参数配置，不配置使用默认值（参考commons-pool2） -->
			</bean>
		</constructor-arg>
	</bean>
	
	<!-- 默认调用了start方法，由Spring容器直接带起来注册到zbus总线上 -->
	<bean id="myrpcService" class="org.zbus.rpc.mq.Service" init-method="start">
		<constructor-arg>  
			<bean class="org.zbus.rpc.mq.ServiceConfig">
			    <!-- 支持多总线注册 -->
				<constructor-arg> 
					<list>
						<ref bean="broker"/> 
					</list>
				</constructor-arg>  
				<property name="mq" value="MyRpc"/>
				<property name="consumerCount" value="2"/> 
				<property name="messageProcessor" ref="serviceProcessor"/>
			</bean>
		</constructor-arg>
	</bean>


### Spring集成--客户端


	<bean id="broker" class="org.zbus.broker.SingleBroker">
		<constructor-arg>
			<bean class="org.zbus.broker.BrokerConfig">
				<property name="serverAddress" value="127.0.0.1:15555" /> 
			</bean>
		</constructor-arg>
	</bean>
	
	<bean id="myrpc" class="org.zbus.rpc.RpcFactory">
		<constructor-arg> 
			<bean class="org.zbus.rpc.mq.MqInvoker"> 
				<constructor-arg ref="broker"/>
				<constructor-arg value="MyRpc"/> 
			</bean>
		</constructor-arg>
	</bean>
 
 
	<bean id="interface" factory-bean="myrpc" factory-method="getService">
		<constructor-arg type="java.lang.Class" value="org.zbus.rpc.biz.Interface"/> 
	</bean> 

**Spring完成zbus代理透明化，zbus设施从你的应用逻辑中彻底消失**

	public static void main(String[] args) { 
		ApplicationContext context = new ClassPathXmlApplicationContext("SpringRpcClient.xml");
		 
		Interface intf = (Interface) context.getBean("interface"); 
		for(int i=0;i<100;i++){
			System.out.println(intf.listMap());
		} 
	} 
	
![zbus](http://git.oschina.net/uploads/images/2016/0316/160036_48bf3b72_7458.png "zbus")