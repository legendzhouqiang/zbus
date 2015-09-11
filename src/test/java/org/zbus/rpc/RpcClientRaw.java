package org.zbus.rpc;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcCodec.Request;
import org.zbus.rpc.mq.MqInvoker;

public class RpcClientRaw {

	public static void main(String[] args) throws Exception {
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
	 
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");
		RpcConfig rpcConfig = new RpcConfig();
		rpcConfig.setVerbose(true);
		RpcInvoker rpc = new RpcInvoker(invoker, rpcConfig);   
		
		
		//同步
		String res = rpc.invokeSync(String.class, "getString", "testxxxx");
		System.out.println(res);
		
		
		//更多配置项目
		Request req = new Request()
			.module("")
			.method("getString")
			.params("testXXX"); 
		
		res = rpc.invokeSync(String.class, req);
		System.out.println(res);
		
		 
		//异步 
		rpc.invokeAsync(String.class, req, new ResultCallback<String>() { 
			@Override
			public void onReturn(String result) { 
				System.out.println("Aysnc: "+ result);
			}
		});
		
		//broker.close(); //异步时候不能立刻关闭 TODO
	}
}
