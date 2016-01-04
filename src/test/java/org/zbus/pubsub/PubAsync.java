package org.zbus.pubsub;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Producer;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message;

public class PubAsync {
	public static void main(String[] args) throws Exception{   
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		 
		Producer producer = new Producer(broker, "MyPubSub", MqMode.PubSub);
		producer.createMQ();  
		
		
		Message msg = new Message();
		msg.setTopic("sse"); 
		
		for(int i=0;i<10000;i++){
			msg.setBody("hello world" + i);
			producer.invokeAsync(msg, new ResultCallback<Message>() {

				@Override
				public void onReturn(Message result) {
					//System.out.println(result);
					//ignore
				}
			});
		}
		
		Thread.sleep(5000); //safe message sending out
		broker.close();
	} 
}
