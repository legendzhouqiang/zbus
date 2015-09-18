package org.zbus.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.mq.MqInvoker;

public class ServiceInvokerExample {
	
	public static void main(String[] args) throws Exception{   
		//配置Broker
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setServerAddress("127.0.0.1:15555"); 
		Broker broker = new SingleBroker(brokerConfig);
		
		//基于MQ的调用
		MessageInvoker invoker = new MqInvoker(broker, "MyService");
		
		Message req = new Message();
		req.setBody("hello world");
		Message res = invoker.invokeSync(req, 2500);
		System.out.println(res);
		
		
		broker.close();
	} 
	
}
