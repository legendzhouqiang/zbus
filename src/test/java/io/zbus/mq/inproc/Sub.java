package io.zbus.mq.inproc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.mq.MqClient;
import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;

public class Sub { 
	
	@SuppressWarnings({ "resource" })
	public static void main(String[] args) throws Exception { 
		MqServer server = new MqServer(new MqServerConfig());
		MqClient client = new MqClient(server);    
		client.heartbeat(30, TimeUnit.SECONDS);
		
		final String mq = "DiskQ", channel = "MyChannel";
		AtomicInteger count = new AtomicInteger(0);  
		client.addListener(mq, channel, data->{
			if(count.getAndIncrement() % 10000 == 0) {
				System.out.println(data); 
			} 
		});  
		
		client.onOpen(()->{
			Map<String, Object> req = new HashMap<>();
			req.put("cmd", "create"); //create MQ/Channel
			req.put("mq", mq); 
			req.put("mqType", "disk"); //Set as Disk type
			req.put("channel", channel);  
			client.invoke(req, res->{
				System.out.println(res);
			});  
			
			Map<String, Object> sub = new HashMap<>();
			sub.put("cmd", "sub"); //Subscribe on MQ/Channel
			sub.put("mq", mq); 
			sub.put("channel", channel);
			client.invoke(sub, res->{
				System.out.println(res);
			});
		});
		
		client.connect();  
	} 
}
