package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public class Create { 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqClient ws = new MqClient("localhost:15555");

		ws.onText = msg -> {
			System.out.println(msg); 
		};

		ws.onOpen = () -> { 
			JSONObject req = new JSONObject();
			req.put("cmd", "create"); 
			
			Map<String, Object> mqInfo = new HashMap<>();
			mqInfo.put("name", "MyMQ");
			
			Map<String, Object> channelInfo = new HashMap<String, Object>();
			channelInfo.put("name", "MyChannel");  
			req.put("mq", mqInfo);            // { name: "MyMQ" }
			req.put("channel", channelInfo);  // { name: "MyChannel" }
			 
			ws.sendMessage(req);
		};

		ws.connect();
	} 
}
