## 一、ZBus服务总线问题领域
> * **企业SOA服务治理**
* **负载均衡横向扩展服务**
* **跨平台多语言适配服务**


### **WebService标准**
* HTTP+XML协议拖沓性能不高，无长链接机制
* 缺乏便捷的服务横向扩容机制
* 缺乏统一的服务视图，多点接入

### 商业与开源ESB
* 商业ESB繁重，做一堆不切合实际的工作，适配文件服务/DB等等，违背专业分工理念
* 开源产品（MuleESB，OpenESB，JBossESB）仍然过重，跟商业产品存在类似的问题。

### ZBus服务总线核心问题解决
> 摈弃所谓的企业级，回归到服务总线，做两件简单的事情：

* **1)只写业务逻辑代码，不写重复的服务接入处理程序**
* Tomcat/Jetty等Web容器是HTTP请求应答模式的一种抽象（Servlet），目标也是尽量只写业务逻辑代码，但是侵入仍然有不少，Servlet框架，还有Struts/SpringMVC等一些列的框架均为简化而生
* Apache/Nginx则更像是对HTTP服务之上的一种抽象，只管提供HTTP接入路由优化
* **ZBus目标之一** 是免去写侦听端口，接收连接，编解码请求包，调用业务逻辑，反馈回客户端中除了业务逻辑代码之外的所有事情。
* **2)透明的横向扩容机制**
* Tomcat等Web容器的Servlet模型解决内部多线程扩容机制，Apahce/Nginx BackServer解决HTTP容器层面的扩容机制
* **ZBus目标之二** 是提供简单的并发工作者模式运行业务逻辑代码，并且对业务逻辑代码透明（单机多线程，多机多线程均可）

> **ZBus追求Z（Zero）的轻量级理念，尽量少的配置，尽量小的设计，跨平台几百K拷贝随时随地运行（Linux简单源码编译）**


### **简单的一个例子**
* 1) 启动zbus总线（本地启动，或者远程服务器上跑）， 200K左右无配置启动
* 2) 编写业务逻辑代码，在main方法中启动注册，完成服务编写【Java平台示例】

```java
public class RpcService { 
	
	public static void main(String[] args) throws Exception {

		RpcServiceHandler handler = new RpcServiceHandler(); 
		//简单添加模块
		handler.addModule("ServiceInterface", new TestService()); 
		
		//注册服务名为MyRpc的服务到ZBus总线上
		ZBusService service = new ZBusService("MyRpc", handler);
		service.setHost("127.0.0.1");
		service.setPort(15555);
		service.setWorkerThreadCount(2); //以2个工作者线程运行
		
		service.run();
		
	}
}
```
其中TestService可以是任何JAVA代码逻辑

* 3)客户端程序
```java
public class ServiceProxyClient {
	public static void main(String[] args) throws Throwable { 
		ServiceInterface rpc = RpcFactory.getService(
				ServiceInterface.class, 
				"zbus://localhost:15555/MyRpc"); 
		
		String pong = rpc.echo();
		System.out.println(pong); 
		
		int c = rpc.plus(1, 2);
		System.out.println(c);
		
		RpcFactory.destroy();
	}
}
```

### ZBus的发展历程
* **第一阶段** 基于ZeroMQ消息通讯框架基础之上，在国信与前海发展到了ZBus3.8，提供C/C++，JAVA，Python，.NET等众多对应的平台API
　　存在的问题：ZeroMQ的封装，底层平台的发展在后期陆续受到制约，比如ZeroMQ中不使用多线程而是用ZeroMQ IPC/InProc在实际研发中并不能有效推进，ZeroMQ对网络事件的封闭封装也很难以基于事件的模式做业务封装。
　　但这些并不能说明ZeroMQ不是个优秀网络通讯框架，ZeroMQ在无Broker的场景下做代码之间的通讯非常合适。
* **第二阶段** 脱离ZeroMQ，部分使用Libevent做为通讯基础（想夸平台解决掉Linux epoll/Windows IOCP/BSD Kqueue等异构问题），当前的开发状态是，离开ZeroMQ除了部分需要做ZeroMQ诸如消息分帧的重复开发之外，获得了更多的自由，也归功与Memcached/Redis等优秀开源项目的源码借鉴
　　整体上两个大版本目前都在支持，但是重点在发展新的去除ZeroMQ的版本，对应用层来说，尤其是JAVA 远程方法调用场景，这两个平台的切换几乎可以做到透明。


### ZBus发展方向
* 借鉴Memcached/Apache的Master-Worker模式，把zbus内核部分进一步多线程改造
* 借鉴FastDFS，加入TrackServer与BusServer的理念，脱离LVS的模式做高可用。


## 二、ZBus设计概览
### ZBus消息最小组织单元不是Service服务槽，而是消息队列，一个请求应答Service由一个服务请求队列+一个服务应答队列组合完成。

***


> 消息队列支持消息发布者PUB发布消息，消息消费者SUB订阅消息。消息队列种类包括：
* 1)Round-Robin Roller分发模式 
* 2)Match准确匹配模式
* 3)Filter主题消息过滤模式
![队列模型](http://git.oschina.net/uploads/images/2014/0413/135225_e1c3253c_7458.png)

> 请求应答服务设计：
* 请求队列采用 Roller分发模式（负载均衡到Worker）
* 应答队列采用Match准确匹配模式（准确返回到服务请求者）
![服务模型](http://git.oschina.net/uploads/images/2014/0413/135201_9609b24a_7458.png)

## 三、消息设计
> 消息由 **消息业务** 与 **消息控制** 两大部分组成
![消息设计](http://git.oschina.net/uploads/images/2014/0413/135218_f7567ef9_7458.png)


### 消息业务
* **状态码**，业务上使用标识消息的状态，比如借用HTTP协议Code，200标识消息成功，400消息格式错误，404消息请求的队列不存在。
* **消息头部**，供业务上扩展，采用key-value格式，本质上是个Hash表。比如客户端添加IP信息可以提供IP=192.168.1.1的键值对，类似HTTP的头部，诸如Cookie
* **消息体**，消息体存储，二进制，具体解释业务上一般有状态码来决定


### 消息控制
* **消息命令**，指定消息命令，比如PUB，SUB，一般不需要指定由API内部决定。
* **消息标识**，指定消息ID，默认可以不给，消息ID在消息流中应该保持不变，由客户端来自行决定是否匹配使用该ID，异步场景下经常使用。
* **消息队列**，指定投递的目标队列的名字，比如SingleMQ，ZBus则尝试找到名为SingleMQ的队列投递此消息。
* **访问控制码**，指定消息投递的目标队列需要的访问控制码，如果不匹配消息默认丢弃，ACK条件下返回403的状态码和原因给消息投递方
* **发送者**，指定消息发送者标识，默认不指定由ZBus内部标识，指定的场景一般在异步配对场景
* **接收者**，指定消息接收者标识，在消息队列为Roller负载均衡模式下一般不指定，Match确定匹配模式下需指定。

## 四、MQ设计

### ZBus MQ队列算法复杂度O(1)目标，队列算法提要
* 1) 消息直接入队列
* 2) 根据接收者获取消息

![MQ设计](http://git.oschina.net/uploads/images/2014/0413/204808_3853071a_7458.png)

* **消息入队列[mq_push_msg]**
* mq_push_msg(mq_t* self, zmsg_t* zmsg)
* 1.1 消息挂接到公共消息队列的尾部（公共消息队列以MQ标识索引）
* 1.2 根据消息的接收者查找接收者私有消息队列，如果不存在创建空
* 1.3 把消息同时挂接到接收者私有队列
* 1.4 公共消息队列的消息增加指向私有消息队列的节点，私有消息队列增加指向公共消息队列的节点（如上图指示）
* 【说明】1.1-1.4的所有操作复杂度均为O(1)

> * **消息出队列[mq_fetch_msg]**
* mq_fetch_msg(mq_t* self,  char* recver)
* 1.1 如果消息队列模式为Roller负载均衡模式，从公共消息队列头部取出消息，同时删除接收者队列中对应的节点，否则进入下面的操作
* 1.2 根据接收者查找对应私有消息队列，删除从消息队列头部取出消息，并将指向的公共消息队列节点清楚
　　

* **增加订阅者[mq_put_recver] [见代码，略]**
* **删除订阅者[mq_rem_recver] [见代码，略]**



## 五、ZBus API（C/C++）

* pub与sub可以分裂，由不同的socket，也可以由相同的socket完成

* **创建客户端** 
  zbus_connect()

* **销毁客户端**
　　zbus_destroy()

* **创建队列**
　　zbus_createmq
　　在ZBus总线上创建队列

* **发布消息**
　　zbus_pub
　　发布PUB消息指令，可选的ACK设置
　　
* **同步订阅消息**
　　zbus_sub
　　发送SUB消息队列指令，等到消息至超时
　　
* **同步请求服务**
　　zbus_call(zmsg) 
　　请求服务，等待返回
　　
* **注册提供服务**
    zbus_serve(service_handler)

## 六、JAVA编写的服务于访问程序示例

以ZBUS实现RPC为例，ZBUS内部都是Binary二进制格式处理数据，具体业务协议格式与ZBUS本身透明。ZBUS的Java API提供了一个默认的基于JSON协议的RPC实现

### 服务端

服务端可以接口化，也可单纯的POJO，没有任何限制，这里采用服务Interface。经过简单代理封装后，服务端与客户端代码几乎与ZBUS无关（透明化总线的存在）

服务接口，客户端只需要这个接口类

```java
public interface ServiceInterface { 
	public String echo(String ping); 
	public int plus(int a, int b);
	public byte[] bin();
}
```

服务具体实现
MyService中唯一给ZBUS侵入的是注解@Remote，用于方便Java API检查哪些方法需要注册，如果采用默认全部注册的规则，则完全可以与ZBUS无关

```java
class MyService implements ServiceInterface {
	@Remote() 
	public String echo(String ping){ 
		return ping;
	}
	@Remote()
	public int plus(int a, int b) {
		return a + b;
	}

	@Remote
	public byte[] bin() {
		byte[] res = new byte[100];
		for (int i = 0; i < res.length; i++)
			res[i] = (byte) i;
		return res;
	}
}
```

最后一步是如何把上述服务注册到ZBUS总线，Java API可以很简单注册如下

```java
public class RpcService {
public static void main(String[] args) throws Exception { 
		RpcServiceHandler handler = new RpcServiceHandler(); 
		//简单添加模块
		handler.addModule("ServiceInterface", new TestService()); 
		
		//注册服务名为MyRpc的服务到ZBus总线上
		ZBusService service = new ZBusService("MyRpc", handler);
		service.setHost("127.0.0.1");
		service.setPort(15555);
		service.setWorkerThreadCount(2); //以2个工作者线程运行
		
		service.run();
	}
}
```

###客户端

通过RpcFactory动态获取ServiceInterface的动态实现类

```java
public class RpcFactoryExample {
public static void main(String[] args) throws Throwable { 
		
		ServiceInterface rpc = RpcFactory.getService(
				ServiceInterface.class, 
				"zbus://localhost:15555/MyRpc"); 
		
		String pong = rpc.echo("ping");
		
		System.out.println(pong); 
	}
}
```