package org.zbus.mq.perf;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.Producer;
import org.zbus.mq.SingleBroker;
import org.zbus.mq.Protocol.MqMode;
import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;

public class PubPerf {
	public static void main(String[] args) throws Exception { 
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("10.19.1.32:15555");
		final Broker broker = new SingleBroker(config);
		
		final long total = 100000;
		
		final AtomicLong counter = new AtomicLong(0);
		Producer p = new Producer(broker, "MyPubSub", MqMode.PubSub, MqMode.Memory);
		p.createMQ();
		
		final long start = System.currentTimeMillis();
		for(int i=0;i<total;i++){
			Message msg = new Message();
			msg.setBody("hello");
			p.sendAsync(msg, new ResultCallback<Message>() { 
				public void onReturn(Message result) { 
					//System.out.println(result);
					counter.incrementAndGet();
					if(counter.get()%1000==0 || counter.get()==total){
						long end = System.currentTimeMillis();
						System.out.println(counter.get()*1000.0/(end-start));
					} 
					if(counter.get() == total){
						try {
							broker.close();
						} catch (IOException e) { 
							e.printStackTrace();
						}
					}
				}
			});
		}
	}
}
