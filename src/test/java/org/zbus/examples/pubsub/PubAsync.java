package org.zbus.examples.pubsub;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.mq.Producer;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;

public class PubAsync {
	public static void main(String[] args) throws Exception{   
		final Broker broker = new ZbusBroker("127.0.0.1:15555");
		 
		Producer producer = new Producer(broker, "MyPubSub", MqMode.PubSub);
		producer.createMQ();  
		 
		Message msg = new Message();
		msg.setTopic("zbus");  
		msg.setBody("hello world " + System.currentTimeMillis());
		
		producer.sendAsync(msg, new ResultCallback<Message>() { 
			@Override
			public void onReturn(Message result) {
				System.out.println(result);
			}
		}); 
		
		
		Thread.sleep(500); //safe message sending out
		broker.close();
	} 
}
