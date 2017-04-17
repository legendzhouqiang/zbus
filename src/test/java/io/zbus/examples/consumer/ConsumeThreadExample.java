package io.zbus.examples.consumer;

import java.io.IOException;

import io.zbus.mq.ConsumeHandler;
import io.zbus.mq.ConsumeThread;
import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.net.EventDriver;

public class ConsumeThreadExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		EventDriver driver = new EventDriver();
		MqClient client = new MqClient("localhost:15555", driver);
		
		ConsumeThread thread = new ConsumeThread(client);
		thread.setTopic("MyTopic");
		thread.setConsumeHandler(new ConsumeHandler() { 
			@Override
			public void handle(Message msg, MqClient client) throws IOException {
				System.out.println(msg);
			}
		});
		
		thread.start();  
	}

}
