package io.zbus.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.zbus.transport.Invoker;
import io.zbus.transport.http.WebsocketClient;

public class WebsocketClientExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		Invoker rpc = new WebsocketClient("localhost:8080");
		
		AtomicInteger count = new AtomicInteger(0);  
		for (int i = 0; i < 100000; i++) {
			Map<String, Object> req = new HashMap<>();
			req.put("method", "getOrder");
			req.put("module", "example");
			
			//Map<String, Object> res = rpc.invoke(req); //sync mode
			
			rpc.invoke(req, res->{
				int c = count.getAndIncrement();
				if(c % 10000 == 0) {
					System.out.println(c + ": " + res);
				}
			});
		}
		//rpc.close();
	}
}
