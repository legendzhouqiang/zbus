package org.zbus.proxy;

import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class ClientPerf {

	public static void main(String[] args) throws Exception { 
		SelectorGroup dispatcher = new SelectorGroup();
		
		
		Message req = new Message();
		req.setCmd("hello");
		req.setBody("hello");
		
		for(int i=0;i<1000;i++){
			MessageClient client = new MessageClient("127.0.0.1:80", dispatcher);
			Message res = client.invokeSync(req);
			System.out.println(">>>"+i+"\n"+res);
			client.close();
		}
		 
		dispatcher.close();
	} 
}
