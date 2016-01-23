package org.zbus.proxy;

import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class ConnectionPerf {

	public static void main(String[] args) throws Exception { 
		SelectorGroup dispatcher = new SelectorGroup();
		
		for(int i=0;i<100000;i++){
			MessageClient client = new MessageClient("127.0.0.1:80", dispatcher);
			
			Message req = new Message();
			req.setCmd("hello");
			req.setBody("hello");
			Message res = client.invokeSync(req);
			System.out.println(res); 
			
			client.close();
			System.out.println(">>>"+i);
		}
		
		dispatcher.close();
	} 
}
