package org.zbus.performance;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ZbusBroker;
import org.zbus.kit.ConfigKit;
import org.zbus.mq.Consumer;
import org.zbus.mq.ConsumerConfig;
import org.zbus.net.http.Message;

public class ConsumerTakeClose {
	public static void main(String[] args) throws Exception{   
		final String serverAddress = ConfigKit.option(args, "-b", "127.0.0.1:15555");
		final String mq = ConfigKit.option(args, "-mq", "MyMQ"); 
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress(serverAddress);
		Broker broker = new ZbusBroker(brokerConfig);
		
		final ConsumerConfig config = new ConsumerConfig(); 
		config.setBroker(broker);
		config.setMq(mq); 
		
		for(int i=0;i<1000000;i++){
			Consumer consumer = new Consumer(config);
			Message message = consumer.take(1000);
			if(message == null){
				consumer.close();
			} else {
				System.out.println(message);
				consumer.close();
			}
		}
		
		broker.close();
	} 
}
