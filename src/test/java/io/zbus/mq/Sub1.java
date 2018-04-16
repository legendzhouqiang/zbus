package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.JsonKit;
import io.zbus.net.EventLoop;

public class Sub1 { 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();  
		MqClient ws = new MqClient("localhost:15555", loop);

		ws.onMessage = msg -> {
			System.out.println(msg); 
		};

		ws.onOpen = () -> { 
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "sub"); 
			req.put("topic", "/abc");    
			req.put("channel", "share1");
			 
			ws.sendMessage(req);
		};

		ws.connect();
		ws.heartbeat(10*1000, ()->{
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "ping");  
			return JsonKit.toJSONString(req);
		});
	} 
}
