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

		for (int i = 0; i < 10; i++) {
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "pub");
			req.put("topic", "/abc");
			req.put("data", i);

			ws.sendMessage(req);
		}

		Thread.sleep(1000);
		ws.close();
		loop.close();
	}
}
