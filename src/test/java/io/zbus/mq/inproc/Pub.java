package io.zbus.mq.inproc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.mq.MqServer;
import io.zbus.mq.MqServerConfig;
import io.zbus.transport.inproc.InprocClient;

public class Pub {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MqServer server = new MqServer(new MqServerConfig());
		InprocClient client = new InprocClient(server.getServerAdaptor()); 
		
		String mq = "DiskQ";
		
		Map<String, Object> create = new HashMap<>();
		create.put("cmd", "create");
		create.put("mq", mq); 
		create.put("mqType", "disk");
		client.invoke(create, res->{
			System.out.println(res);
		});
		Thread.sleep(100);
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 1000000; i++) {   
			Map<String, Object> msg = new HashMap<>();
			msg.put("cmd", "pub"); //Publish
			msg.put("mq", mq);
			msg.put("body", i);  
			
			client.invoke(msg, res->{
				if(count.getAndIncrement() % 10000 == 0) {
					System.out.println(res); 
				}
			});
		} 
		//ws.close(); 
	}
}
