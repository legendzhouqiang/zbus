package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Pub2 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MqClient client = new MqClient("localhost:15555"); 
		
		client.heartbeat(30, TimeUnit.SECONDS);
		
		final String mq = "DiskMQ";
		Map<String, Object> req = new HashMap<>();
		req.put("cmd", "create");
		req.put("mq", mq); 
		req.put("mqType", "disk");
		
		client.invoke(req, res->{
			System.out.println(res);
		});
		Thread.sleep(100);
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 100000; i++) {   
			Map<String, Object> msg = new HashMap<>();
			msg.put("cmd", "pub"); //Publish
			msg.put("mq", mq);
			msg.put("body", i); //set business data
			
			client.invoke(msg, res->{
				if(count.getAndIncrement() % 10000 == 0) {
					System.out.println(res); 
				}
			});
		} 
		//ws.close(); 
	}
}
