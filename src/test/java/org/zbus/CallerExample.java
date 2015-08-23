package org.zbus;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageInvoker;
import org.zbus.rpc.broking.BrokingInvoker;

public class CallerExample {
	public static void main(String[] args) throws Exception { 
		for (int i = 0; i < 1; i++) {
			test();
		}
	}

	public static void test() throws Exception { 
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		MessageInvoker invoker = new BrokingInvoker(broker, "MyService");

		Message msg = new Message();
		msg.setBody("hello world");
		for (int i = 0; i < 1000000; i++) {
			Message res = invoker.invokeSync(msg, 2500);
			System.out.println(res);
		}

		broker.close();
	}
}
