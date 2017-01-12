package io.zbus.performance;

import io.zbus.mq.Broker;
import io.zbus.mq.Consumer;
import io.zbus.mq.Message;
import io.zbus.mq.MqConfig;
import io.zbus.mq.broker.BrokerConfig;
import io.zbus.mq.broker.ZbusBroker;
import io.zbus.util.ConfigUtil;

public class ConsumerTakeClose {
	public static void main(String[] args) throws Exception{   
		final String serverAddress = ConfigUtil.option(args, "-b", "127.0.0.1:15555");
		final String mq = ConfigUtil.option(args, "-mq", "MyMQ"); 
		
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress(serverAddress);
		Broker broker = new ZbusBroker(brokerConfig);
		
		final MqConfig config = new MqConfig(); 
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
