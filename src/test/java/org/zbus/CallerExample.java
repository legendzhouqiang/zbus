package org.zbus;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.mq.MqInvoker;

public class CallerExample {
	public static void main(String[] args) throws Exception { 
		for (int i = 0; i < 1; i++) {
			test();
		}
	}

	public static void test() throws Exception { 
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		MessageInvoker invoker = new MqInvoker(broker, "MyService");

		Message msg = new Message();
		msg.setBody("hello world");
		for (int i = 0; i < 1000000; i++) {
			Message res = invoker.invokeSync(msg, 2500); 
			System.out.println(res);
		}

		broker.close();
	}
}
