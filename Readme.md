#ZBus服务总线问题领域
* 企业内部标准化服务提供接口（WebService重，限制多,需要集中接入点/服务点）
* 跨平台/语言互访，代理模式解决遗留系统标准化
* 提供透明的负载均衡机制，方便服务横向扩展
* 提供相对透明的DMZ隔离，把服务提供者通过总线做安全隔离
* 服务本身不侦听任何端口，仅仅创建连接到ZBUS总线，网络设备可做安全策略隔离，比如不允许APP网段直接连接数据库等

## 一、ZBUS总线启动（Windows/Linux跨平台）

* Windows默认双击可执行文件zbus.exe
* Linux直接 ./zbus
* 高级配置项通过 -help打印直接配置

## 二、JAVA编写的服务于访问程序示例

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
public static void main(String[] args) throws InterruptedException { 
		WorkerPoolConfig cfg = new WorkerPoolConfig();
		cfg.setService("MyRpc"); 
		cfg.setBrokers(new String[] { "127.0.0.1:15555" });
		int threadCount = 2;
		WorkerPool wc = new WorkerPool(cfg);
		System.out.format("Pooled RPC(%d) Run...\n", threadCount);

		RpcServiceHandler handler = new RpcServiceHandler();  	
		handler.addModule("ServiceInterface", new MyService());  
		wc.run(threadCount, handler); 
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

##三、ZBUS内部文件组成说明

* dist -- 已经发行的ZBUS总线可执行文件，Linux/Windows均支持，Mac需源码编译
* msvc -- Visual Studio 2008项目编译文件，可编译zbus.exe可执行文件（x32/x64）
* redhat -- Linux下make文件，可直接make执行编出目标可执行zbus文件
* src/include, src/zmq -- zeromq 打包入的头和源文件
* src/zbox -- 工具类C源码，包括跨平台宏/链表/Hash等工具类
* src/zbus.cpp, src/zbus.h 真正意义上的zbus总线内部处理逻辑代码（2000行以内）

