package org.zbus.ha;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ha.HaBroker;
import org.zbus.mq.Producer;
import org.zbus.net.http.Message;

public class Producer1 {
	public static void main(String[] args) throws Exception { 
		//创建Broker代理
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setTrackServerList("127.0.0.1:16666");
		Broker broker = new HaBroker(brokerConfig);
 
		Producer producer = new Producer(broker, "MyMQ");
		//producer.createMQ(); // 如果已经确定存在，不需要创建
 
		for(int i=0; i<1000000;i++){
			Message msg = new Message();
			msg.setBody("hello world, from HA broker "+ System.currentTimeMillis());
			try{
				Message res = producer.sendSync(msg);  
				System.out.println(res);
			}catch (Exception e){
				e.printStackTrace();
			} 
			Thread.sleep(1000);
		}
		
		System.out.println("===DONE===");
		//销毁Broker
		broker.close();
	}
}
