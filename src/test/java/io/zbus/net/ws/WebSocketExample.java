package io.zbus.net.ws;

import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.JsonKit;
import io.zbus.net.http.WebsocketClient;

public class WebSocketExample {
 
	public static void main(String[] args) throws Exception {  
		
		WebsocketClient ws = new WebsocketClient("localhost"); 
		ws.onText = data -> {
			 System.out.println(data);
			 ws.close();
		};   
		
		ws.onOpen = ()->{
			Map<String, Object> command = new HashMap<>();
			command.put("module", "example");
			command.put("method", "getOrder");   
			ws.sendMessage(JsonKit.toJSONString(command));
			
		};
		
		ws.connect();  
	}
}
