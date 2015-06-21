package org.zstacks.zbus;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.service.Caller;
import org.zstacks.znet.Message;

public class CallerExample {
	public static void main(String[] args) throws IOException {
		for (int i = 0; i < 1; i++) {
			test();
		}
	}

	public static void test() throws IOException { 
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
 
		Caller caller = new Caller(broker, "MyService");

		Message msg = new Message();
		msg.setBody("hello world");
		for (int i = 0; i < 10000; i++) {
			Message res = caller.invokeSync(msg, 2500);
			System.out.println(res);
		}

		broker.close();
	}
}
