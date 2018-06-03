package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Sub1 { 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqClient client = new MqClient("localhost:15555");   
		
		client.heartbeat(30, TimeUnit.SECONDS);
		
		final String mq = "MemQ", channel = "MyChannel";
		AtomicInteger count = new AtomicInteger(0);  
		client.addListener(mq, channel, data->{
			int c = count.getAndIncrement();
			if(c%10000 == 0) {
				System.out.println(c + ":" + data);  
			}
		});  
		
		client.onOpen = ()->{
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "create"); //create MQ/Channel
			req.put("mq", mq); 
			req.put("channel", channel);  
			//req.put("offset", 0);
			client.invoke(req, res->{
				System.out.println(res);
			});  
			
			Map<String, Object> sub = new HashMap<>();
			sub.put("cmd", "sub"); //Subscribe on MQ/Channel
			sub.put("mq", mq); 
			sub.put("channel", channel);
			client.invoke(sub, res->{
				System.out.println(res);
			});
		};
		
		client.connect();  
	} 
}
