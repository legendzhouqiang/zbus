## 一、ZBUS服务总线问题领域
> * **企业SOA服务治理**
* **负载均衡横向扩展服务**
* **跨平台多语言适配服务**
* **高可用、高并发**

> **ZBUS追求高度轻量级便捷使用，200K左右的jar包（从早期的基于ZeroMQ C实现演化为JAVA NIO实现），不依赖任何其他包，高度可扩展（日志、JSON协议格式等等），ZBUS兼容HTTP协议标准（协议本身兼容，不是适配）API包涵C/C++,C#, JAVA, Python等平台轻量级接入，并提供丰富的示例**

### **TODO**
* 1) 增加C、Python平台异步操作API
* 2) 增加Node.JS平台接入


### **简单的一个例子**
* 1) 启动zbus总线（本地启动，或者远程服务器上跑）， 200K左右无配置启动
* 2) 编写业务逻辑代码，在main方法中启动注册，完成服务编写【Java平台示例】

```java

public class RpcServiceExample {
	
	public static void main(String[] args) throws Exception {  
		String broker = Helper.option(args, "-b", "127.0.0.1:15555"); 
		
		int threadCount = Helper.option(args, "-c", 1);
		String service = Helper.option(args, "-s", "MyRpc");
		
		RpcServiceConfig config = new RpcServiceConfig();
		config.setThreadCount(threadCount); 
		config.setServiceName(service);
		config.setBroker(broker); 
		
		config.setServiceHandler(new ServiceHandler() { 
			@Override
			public Message handleRequest(Message request) { 
				System.out.println(request);
				Message result = new Message();
				result.setStatus("200");
				result.setBody("Server time: "+System.currentTimeMillis());
				
				return result;
			}
		});
	
		
		RpcService svc = new RpcService(config);
		svc.start();  
	} 
}
```
其中TestService可以是任何JAVA代码逻辑

* 3)客户端程序
```java
public class RpcSync {

	public static void main(String[] args) throws Exception {  
		final RemotingClient client = new RemotingClient("127.0.0.1", 15555);
		
		Rpc rpc = new Rpc(client, "MyRpc");  
		
		for(int i=0;i<10;i++){
			Message req = new Message(); 
			req.setBody("hello from client "+i); 
			
			Message reply = rpc.invokeSync(req, 10000); 
			System.out.println(reply);
		}
		
		System.out.println("--done--");
	}
}
```


