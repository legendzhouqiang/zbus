package io.zbus.mq;

import java.util.concurrent.atomic.AtomicInteger;

public class Pub {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		MqClient ws = new MqClient("localhost:15555"); 
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 100000; i++) {  
			Object data = i;
			ws.publish("MyMQ", data, res->{
				if(count.getAndIncrement() % 10000 == 0) {
					System.out.println(res); 
				}
			});
		} 
		//ws.close(); 
	}
}
