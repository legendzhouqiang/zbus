package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Pub2 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MqClient client = new MqClient("localhost:15555"); 
		
		final String mq = "DiskMQ";
		Map<String, Object> create = new HashMap<>();
		create.put("cmd", "create");
		create.put("mq", mq); 
		client.invoke(create, res->{
			System.out.println(res);
		});
		Thread.sleep(1000);
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 100000; i++) {   
			Map<String, Object> msg = new HashMap<>();
			msg.put("cmd", "pub"); //Publish
			msg.put("mq", mq);
			msg.put("data", i); //set business data
			
			client.invoke(msg, res->{
				if(count.getAndIncrement() % 10000 == 0) {
					System.out.println(res); 
				}
			});
		} 
		//ws.close(); 
	}
}
