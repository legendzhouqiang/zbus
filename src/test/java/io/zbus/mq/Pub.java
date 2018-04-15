package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

import io.zbus.net.EventLoop;

public class Pub {
 
	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();  
		MqClient ws = new MqClient("localhost:15555", loop);

		ws.onMessage = msg -> {
			System.out.println(msg); 
		};

		Map<String, Object> req = new HashMap<>();
		req.put("cmd", "pub"); 
		req.put("topic", "/abc"); 
		req.put("data", System.currentTimeMillis());
		 
		ws.sendMessage(req);
		
		Thread.sleep(1000);
		ws.close();
		loop.close();
	} 
}
