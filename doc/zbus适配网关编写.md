# ZBUS网关示例--将已有服务改造挂接到zbus上

## 原理说明
zbus网关的应用场景主要是讲某个已有服务适配挂接成zbus的某个服务，做为zbus的某个RPC服务，扮演服务总线的角色

数据流：

* zbus  ==> gateway consumer(request) ==> target server
* zbus  <== gateway router (response) <== target server

网关consumer从指定的消息队列，假设ID为Gateway上消费所有的请求消息，消息有唯一标识+发送者ID，发送给目标服务器
从目标服务器返回的响应消息封装后根据请求时标识的，把源消息ID与发送者ID附加上，交予zbus路由给发送请求者

需要用的核心API：
* 1） consumer.take() //从MQ上取回消息
* 2） consumer.route()  //将消息返回给zbus，由其根据设定好的发送者ID与消息ID路由


需要注意的是，网关有同步网关与异步网关之分，同步网关往往程序简单，但是容易出现网关阻塞。实际生产上，一般都主张使用异步网关，
主要特征是在网关的网络通讯部分全部使用异步调用，避免应用层的异常把网关堵塞住。


## 第一步： 构建一个简单的TargetServer （具有真正的TargetServer省略这一步）

	public static void main(String[] args) throws IOException { 
		SelectorGroup group = new SelectorGroup(); 
		Server server = new Server(group); 
		server.start(8080, new MessageAdaptor(){ 
			@Override
			public void onMessage(Object obj, Session sess) throws IOException {
				Message msg = (Message)obj;
				
				msg.setResponseStatus(200);
				msg.setBody(""+System.currentTimeMillis());
				
				sess.write(msg);
			}
		});  
	} 

上述代码是借助zbus的网络包功能实现的一个简单的HTTP服务，收到HTTP消息，直接返回200，当前服务器时间。


## 第二步： 编写zbus网关程序

个性化Consumer的MessageHandler
	
	public static class GatewayMessageHandler implements MessageHandler{
		//TargetServer messaging 
		SelectorGroup group = new SelectorGroup();
		MessageClient targetClient; //换成非例子中其他目标处理链接模式
		public GatewayMessageHandler() throws IOException{ 
			targetClient = new MessageClient("127.0.0.1:8080", group);
		}
		
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			//msgId should be maintained
			final Session zbusSess = sess;
			final String msgId = msg.getId(); //记录下消息ID
			final String sender = msg.getSender(); //记录下消息发送者ID
			targetClient.invokeAsync(msg, new ResultCallback<Message>() {

				@Override
				public void onReturn(Message result) { 
					result.setCmd(Protocol.Route);
					result.setRecver(sender); //指定接受者ID
					result.setId(msgId); //指定消息匹配源ID
					result.setAck(false); //make sure no reply message required
					try {
						zbusSess.write(result);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			
		}
	}

过程中如果不能方便的透传msgID到目标服务器并返回，则需要建立路由表，具体的可以参见zbus对接微软MSMQ的C++程序
