package org.zbus.examples.gateway;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.rpc.RpcCodec.Request;
import org.zbus.rpc.RpcCodec.Response;
import org.zbus.rpc.RpcInvoker;
import org.zbus.rpc.mq.RpcGatewayHandler;
import org.zbus.rpc.mq.Service;
import org.zbus.rpc.mq.ServiceConfig;

class MyGatewayRpcHandler extends RpcGatewayHandler {

	private Broker broker;
	private RpcInvoker rpcInvoker;
	public MyGatewayRpcHandler() throws IOException{
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:8080");
		
		broker = new SingleBroker();
		rpcInvoker = new RpcInvoker(broker);
	}
	
	@Override
	protected void onRequest(Request req, final Message rawMsg, final Session reqSession) {
		 rpcInvoker.invokeAsync(req, new ResultCallback<Response>() { 
			@Override
			public void onReturn(Response result) { 
				onResponse(result.getResult(), result.getError(), rawMsg, reqSession);
			}
		}); 
	} 
}

public class GatewayServer {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		Broker broker = new SingleBroker();
		ServiceConfig config = new ServiceConfig();
		config.setBroker(broker);
		config.setMq("MyGateway");
		config.setConsumerCount(4);

		MessageHandler handler = new MyGatewayRpcHandler();
		config.setMessageHandler(handler);

		Service service = new Service(config);
		service.start();
	}

}
