package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Pub {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MqClient ws = new MqClient("localhost:15555"); 
		
		AtomicInteger count = new AtomicInteger(0);
		ws.onText = msg -> {
			if(count.getAndIncrement() % 10000 == 0) {
				System.out.println(msg); 
			}
		};

		for (int i = 0; i < 500000; i++) {
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "pub");
			req.put("mq", "MyMQ");
			req.put("data", i);

			ws.sendMessage(req);
		} 
		//ws.close(); 
	}
}
