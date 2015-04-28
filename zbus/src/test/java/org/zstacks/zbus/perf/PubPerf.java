package org.zstacks.zbus.perf;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.Producer;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.protocol.MessageMode;
import org.zstacks.znet.Message;
import org.zstacks.znet.ticket.ResultCallback;

public class PubPerf {
	public static void main(String[] args) throws Exception { 
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		final long total = 100000;
		
		final AtomicLong counter = new AtomicLong(0);
		Producer p = new Producer(broker, "MyPubSub", MessageMode.PubSub, MessageMode.Temp);
		p.createMQ();
		
		final long start = System.currentTimeMillis();
		for(int i=0;i<total;i++){
			Message msg = new Message();
			msg.setBody("hello");
			p.send(msg, new ResultCallback() { 
				public void onCompleted(Message result) { 
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
