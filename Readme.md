# zbus--轻量级消息队列、服务总线


##**zbus** 特性


* **消息队列 -- 生产者消费者模式、发布订阅**
* **服务总线 -- 适配改造已有业务系统，使之具备跨平台与语言, RPC**
* **RPC -- 分布式远程方法调用，Java方法透明代理**
* **跨平台、多语言**
* **轻量级、高可用**


##**zbus** 项目结构


###**zbus**通讯基础之znet

[轻量级、高性能NIO网络通讯框架](http://git.oschina.net/rushmore/znet "znet") 


### **zbus** API
* [Java API](http://git.oschina.net/rushmore/zbus/tree/master/src/main/java/org/zstacks/zbus/client "zbus") 
* [C/C++ API](http://git.oschina.net/rushmore/zbus-api/tree/master/zbus-api-c "zbus-api-c") 
* [Python API](http://git.oschina.net/rushmore/zbus-api/tree/master/zbus-api-python "zbus-api-python") 
* [C# API](http://git.oschina.net/rushmore/zbus-api/tree/master/zbus-api-csharp "zbus-api-csharp") 
* [Node.JS API](http://git.oschina.net/rushmore/zbus-api/tree/master/zbus-api-nodejs "zbus-api-nodejs") 

###**zbus** 桥接

* [微软MSMQ|国信交易调度](http://git.oschina.net/rushmore/zbus-proxy/tree/master/zbus-proxy-msmq "zbus-proxy-msmq") 
* [金证KCXP](http://git.oschina.net/rushmore/zbus-proxy/tree/master/zbus-proxy-kcxp "zbus-proxy-kcxp") 
* [国信TC](http://git.oschina.net/rushmore/zbus-proxy/tree/master/zbus-proxy-tc "zbus-proxy-tc") 
* [桥接JAVA客户端](http://git.oschina.net/rushmore/zbus-proxy/tree/master/zbus-proxy-java "zbus-proxy-java") 


## zbus 启动与监控 

1. zbus-dist选择zbus.sh或者zbus.bat直接执行
2. 通过源码zbusServer.java个性化控制启动

![简单监控](http://git.oschina.net/uploads/images/2015/0212/103207_b5d2e1d3_7458.png)

总线默认占用**15555**端口， [http://localhost:15555](http://localhost:15555 "默认监控地址") 可以直接进入监控，注意zbus因为原生兼容HTTP协议所以监控与消息队列使用同一个端口

**高可用模式启动总线**
分别启动zbusServer与TrackServer，无顺序之分，默认zbusServer占用15555端口，TrackServer占用16666端口。


## zbus 设计概要图

![zbus-arch](http://git.oschina.net/uploads/images/2015/0419/134134_62a4e21c_7458.png)


## zbus 示例

### Java Maven 依赖

	<dependency>
		<groupId>org.zstacks</groupId>
		<artifactId>zbus</artifactId>
		<version>6.0.0-SNAPSHOT</version>
	</dependency>

### 生产者


		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 创建生产者 【轻量级对象，不需要释放，随便使用】
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ(); //如果已经确定存在，不需要创建
		
		Message msg = new Message(); 
		msg.setBody("hello world");  
		Message res = producer.sendSync(msg, 1000);
		System.out.println(res);
		
		//3）销毁Broker
		broker.close();


### 消费者

		//1）创建Broker代表
		SingleBrokerConfig brokerConfig = new SingleBrokerConfig();
		brokerConfig.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);
		
		MqConfig config = new MqConfig(); 
		config.setBroker(broker);
		config.setMq("MyMQ");
		
		//2) 创建消费者
		@SuppressWarnings("resource")
		Consumer c = new Consumer(config);
		
		c.onMessage(new MessageCallback() {
			public void onMessage(Message msg, Session sess) throws IOException {
				System.out.println(msg);
			}
		});

 
### RPC动态代理【各类复杂类型】

参考源码test目下的rpc部分

		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		RpcConfig rpcConfig = new RpcConfig();
		rpcConfig.setBroker(broker);
		rpcConfig.setMq("MyRpc"); 
		
		//动态代理处Interface通过zbus调用的动态实现类
		Interface hello = RpcProxy.getService (Interface.class, rpcConfig);

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




 
 
### Spring集成--服务端(RPC示例)

**无任何代码侵入使得你已有的业务接口接入到zbus，获得跨平台和多语言支持**

	<!-- 暴露的的接口实现示例 -->
	<bean id="interface" class="org.zstacks.zbus.rpc.biz.InterfaceImpl"></bean>
	
	<bean id="serviceHandler" class="org.zstacks.zbus.client.rpc.RpcServiceHandler">
		<constructor-arg>
			<list>
				<!-- 放入你需要暴露的接口 ,其他配置基本不变-->
				<ref bean="interface"/>
			</list>
		</constructor-arg>
	</bean>
	
	<!-- 切换至高可用模式，只需要把broker的实现改为HaBroker配置 -->
	<bean id="broker" class="org.zstacks.zbus.client.broker.SingleBroker">
		<constructor-arg>
			<bean class="org.zstacks.zbus.client.broker.SingleBrokerConfig">
				<property name="brokerAddress" value="127.0.0.1:15555" />
			</bean>
		</constructor-arg>
	</bean>
	
	<!-- 默认调用了start方法，由Spring容器直接带起来注册到zbus总线上 -->
	<bean id="zbusService" class="org.zstacks.zbus.client.service.Service" init-method="start">
		<constructor-arg>  
			<bean class="org.zstacks.zbus.client.service.ServiceConfig">
				<property name="broker" ref="broker"/>
				<property name="mq" value="MyRpc"/>
				<property name="threadCount" value="2"/>
				<property name="serviceHandler" ref="serviceHandler"/>
			</bean>
		</constructor-arg>
	</bean>
	


### Spring集成--客户端

	<!-- 切换至高可用模式，只需要把broker的实现改为HaBroker配置 -->
	<bean id="broker" class="org.zstacks.zbus.client.broker.SingleBroker">
		<constructor-arg>
			<bean class="org.zstacks.zbus.client.broker.SingleBrokerConfig">
				<property name="brokerAddress" value="127.0.0.1:15555" />
			</bean>
		</constructor-arg>
	</bean>
	

	<!-- 动态代理由RpcProxy的getService生成，需要知道对应的MQ配置信息（第二个参数） -->
	<bean id="interface" class="org.zstacks.zbus.client.rpc.RpcProxy" factory-method="getService">
		<constructor-arg type="java.lang.Class" value="org.zstacks.zbus.rpc.biz.Interface"/> 
		<constructor-arg>
			<bean class="org.zstacks.zbus.client.rpc.RpcConfig">
				<property name="broker" ref="broker"/> 
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
	

## zbus消息协议

* [zbus消息协议](http://git.oschina.net/rushmore/zbus/wikis/zbus%E6%B6%88%E6%81%AF%E5%8D%8F%E8%AE%AE- "zbus-protocol") 

