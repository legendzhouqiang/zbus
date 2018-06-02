package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

public class Get {  
	
	public static void main(String[] args) throws Exception { 
		MqClient client = new MqClient("localhost:15555");
		
		Map<String, Object> req = new HashMap<>();
		req.put("cmd", "get");  
		req.put("mq", "MyMQ"); 
		req.put("channel", "MyChannel"); 
		
		client.invoke(req, res->{
			System.out.println(res);
			
			client.close();
		});  
	} 
}
