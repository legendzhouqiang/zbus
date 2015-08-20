# ZBUS--轻量级MQ、RPC、服务总线


##ZBUS = MQ + RPC + PROXY


* **MQ -- 生产消费模式（持久化）**
* **PubSub -- 发布订阅模式**
* **RPC -- 远程方法调用**
* **服务代理 -- 适配改造已有业务系统，使之具备跨平台与语言**
* **超轻量级 单个Jar无依赖 ~190K**
* **跨平台、多语言支持** 

## QQ群: 467741880



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
		<version>6.2.0-SNAPSHOT</version>
	</dependency>

### 生产者


		//创建Broker代理
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); // 如果已经确定存在，不需要创建

		//创建消息，消息体可以是任意binary，应用协议交给使用者
		Message msg = new Message();
		msg.setBody("hello world");
		producer.sendSync(msg);  
		
		//销毁Broker
		broker.close();


### 消费者

		//创建Broker代表
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress("127.0.0.1:15555");
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

 
### RPC动态代理【各类复杂类型】

参考源码test目下的rpc部分

		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);

		RpcProxy proxy = new RpcProxy(broker); 
		
		RpcConfig config = new RpcConfig();
		config.setMq("MyRpc"); 
		config.setTimeout(10000);  
		
		Interface hello = proxy.getService(Interface.class, config);


		Object[] res = hello.objectArray();
		for (Object obj : res) {
			System.out.println(obj);
		}

		Object[] array = new Object[] { getUser("rushmore"), "hong", true, 1,
				String.class };
		
		
		int saved = hello.saveObjectArray(array);
		System.out.println(saved);
		 
		Class<?> ret = hello.classTest(String.class);
		System.out.println(ret);
		
		
		broker.close();




 
 
### Spring集成--服务端(RPC示例)

**无任何代码侵入使得你已有的业务接口接入到zbus，获得跨平台和多语言支持**

	<!-- 暴露的的接口实现示例 -->
	
	<bean id="interface" class="org.zbus.rpc.biz.InterfaceImpl"></bean>
	
	<bean id="serviceHandler" class="org.zbus.rpc.RpcServiceHandler">
		<constructor-arg>
			<list>
				<!-- 放入你需要的暴露的的接口 -->
				<ref bean="interface"/>
			</list>
		</constructor-arg>
	</bean>
	 
	<bean id="broker" class="org.zbus.mq.SingleBroker">
		<constructor-arg>
			<bean class="org.zbus.mq.BrokerConfig">
				<property name="brokerAddress" value="127.0.0.1:15555" />
				<property name="maxTotal" value="20"/>
				<!-- 这里可以增加连接池参数配置，不配置使用默认值（参考commons-pool2） -->
			</bean>
		</constructor-arg>
	</bean>
	
	<!-- 默认调用了start方法，由Spring容器直接带起来注册到zbus总线上 -->
	<bean id="zbusService" class="org.zbus.rpc.service.Service" init-method="start">
		<constructor-arg>  
			<bean class="org.zbus.rpc.service.ServiceConfig">
			    <!-- 支持多总线注册 -->
				<constructor-arg> 
					<list>
						<ref bean="broker"/> 
					</list>
				</constructor-arg>  
				<property name="mq" value="MyRpc"/>
				<property name="consumerCount" value="2"/>
				<property name="threadCount" value="20"/>
				<property name="serviceHandler" ref="serviceHandler"/>
			</bean>
		</constructor-arg>
	</bean>


### Spring集成--客户端

	 
	<bean id="broker" class="org.zbus.mq.SingleBroker">
		<constructor-arg>
			<bean class="org.zbus.mq.BrokerConfig">
				<property name="brokerAddress" value="127.0.0.1:15555" />
				<property name="maxTotal" value="20"/>
				<!-- 这里可以增加连接池参数配置，不配置使用默认值（参考commons-pool2） -->
			</bean>
		</constructor-arg>
	</bean>
	
	<bean id="rpcProxy" class="org.zbus.rpc.RpcProxy">
		<constructor-arg> <ref bean="broker"/> </constructor-arg>
	</bean>

	<!-- 动态代理由RpcProxy的getService生成，需要知道对应的MQ配置信息（第二个参数） -->
	<bean id="interface" factory-bean="rpcProxy" factory-method="getService">
		<constructor-arg type="java.lang.Class" value="org.zbus.rpc.biz.Interface"/> 
		<constructor-arg>
			<bean class="org.zbus.rpc.RpcConfig"> 
				<property name="mq" value="MyRpc"/> 
			</bean>
		</constructor-arg>
	</bean>
	 

**Spring完成zbus代理透明化，zbus设施从你的应用逻辑中彻底消失**

	public static void main(String[] args) { 
		ApplicationContext context = new ClassPathXmlApplicationContext("zbusSpringClient.xml");
		
		Interface intf = (Interface) context.getBean("interface");
		
		System.out.println(intf.listMap());
	}
	

## ZBUS消息协议

* [08 ZBUS高阶话题--协议](http://git.oschina.net/rushmore/zbus/blob/master/doc/08.%20ZBUS%E9%AB%98%E9%98%B6%E8%AF%9D%E9%A2%98--%E5%8D%8F%E8%AE%AE.md?dir=0&filepath=doc%2F08.+ZBUS%E9%AB%98%E9%98%B6%E8%AF%9D%E9%A2%98--%E5%8D%8F%E8%AE%AE.md&oid=61c459ed0b6dcb0b6d204711cb4d58d183715a3a&sha=9473c1b43089291e385b15eb3deaa32f7277a428 "08 ZBUS高阶话题--协议") 