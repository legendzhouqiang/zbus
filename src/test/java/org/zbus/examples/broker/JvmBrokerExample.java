package org.zbus.examples.broker;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.JvmBroker;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.Consumer;
import org.zbus.mq.Consumer.ConsumerHandler;
import org.zbus.mq.Producer;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;

public class JvmBrokerExample {
	private static final Logger logger = LoggerFactory.getLogger(JvmBrokerExample.class);
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Broker broker = new JvmBroker();
		
		Consumer consumer = new Consumer(broker, "MyMQ");  
		consumer.createMQ();
		consumer.start(new ConsumerHandler() { 
			@Override
			public void handle(Message msg, Consumer consumer) throws IOException { 
				logger.info(""+msg);
				final String mq = msg.getMq();
				final String msgId  = msg.getId();
				final String sender = msg.getSender();
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) { 
					e.printStackTrace();
				}
				
				
				
				Message res = new Message(); 
				res.setId(msgId);
				res.setMq(mq);  
				res.setRecver(sender); 
				res.setBody("From consumer" + System.currentTimeMillis());
				
				consumer.routeMessage(res);
			}
		});  
		
		Producer producer = new Producer(broker, "MyMQ");
		for(int i=0;i<2;i++) {
			Message message = new Message();
			message.setBody("test body");
			message.setAck(false);
			producer.sendAsync(message, new ResultCallback<Message>() { 
				@Override
				public void onReturn(Message result) {
					System.out.println(result);
				}
			}); 
		} 
		
	} 
	
}
