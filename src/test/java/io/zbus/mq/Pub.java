package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

public class Pub {

	public static void main(String[] args) throws Exception {
		MqClient ws = new MqClient("localhost:15555"); 

		ws.onText = msg -> {
			System.out.println(msg); 
		};

		for (int i = 0; i < 10000; i++) {
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "pub");
			req.put("mq", "MyMQ");
			req.put("data", i);

			ws.sendMessage(req);
		} 
		//ws.close(); 
	}
}
