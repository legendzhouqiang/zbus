package org.zbus.examples.local;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.mq.Consumer;
import org.zbus.mq.Producer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.local.LocalBroker;
import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;
import org.zbus.net.http.Message;

public class LocalExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MqServerConfig config = new MqServerConfig();  
		MqServer server = new MqServer(config);  
		
		Broker broker = new LocalBroker(server); 
 
		Consumer consumer = new Consumer(broker, "MyMQ"); 
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				System.out.println(msg);
			}
		});  
		
		Producer producer = new Producer(broker, "MyMQ");
		producer.createMQ();
		
		for(int i=0;i<10;i++){
			Message msg = new Message();
			msg.setBody("hello world"+i); 
			msg = producer.sendSync(msg); 
			//Thread.sleep(1000);
		} 
		
		//consumer.close();
		//server.close();
	}

}
