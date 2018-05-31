package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Sub1 { 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		MqClient ws = new MqClient("localhost:15555");   
		
		ws.heartbeat(30, TimeUnit.SECONDS);
		
		final String mq = "MyMQ", channel = "MyChannel";
		
		Map<String, Object> req = new HashMap<>();
		req.put("cmd", "create");
		req.put("mq", mq); 
		req.put("channel", channel);
		
		//create MQ/Channel
		ws.invoke(req, res->{
			System.out.println(res);
			ws.subscribe(mq, channel, data->{
				System.out.println(data);
			});  
		});  
		
	} 
}
