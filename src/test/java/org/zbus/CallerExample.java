package org.zbus;

import java.io.IOException;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.Message;
import org.zbus.pool.Pool;
import org.zbus.pool.impl.DefaultPoolFactory;
import org.zbus.rpc.service.Caller;

public class CallerExample {
	public static void main(String[] args) throws IOException {
		Pool.setPoolFactory(new DefaultPoolFactory());
		for (int i = 0; i < 1; i++) {
			test();
		}
	}

	public static void test() throws IOException { 
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		Caller caller = new Caller(broker, "MyService");

		Message msg = new Message();
		msg.setBody("hello world");
		for (int i = 0; i < 1; i++) {
			Message res = caller.invokeSync(msg, 2500);
			System.out.println(res);
		}

		broker.close();
	}
}
