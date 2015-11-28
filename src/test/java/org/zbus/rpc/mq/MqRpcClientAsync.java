package org.zbus.rpc.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.rpc.RpcInvoker;
import org.zbus.rpc.RpcCodec.Request;

public class MqRpcClientAsync {

	public static void main(String[] args) throws Exception {
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);

		MqInvoker messageInvoker = new MqInvoker(broker, "MyRpc");
		RpcInvoker rpc = new RpcInvoker(messageInvoker);

		Request req = new Request()
				.method("plus")
				.params(1,2); 
		
		for(int i=0;i<100000;i++){
		rpc.invokeAsync(Integer.class, req, new ResultCallback<Integer>() {
			@Override
			public void onReturn(Integer result) {
				//System.out.println("Aysnc: " + result);
			}
		});
		}

		Thread.sleep(1000000);
		broker.close();
	}
}
