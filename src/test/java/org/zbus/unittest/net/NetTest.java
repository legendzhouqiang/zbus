package org.zbus.unittest.net;

import java.io.IOException;

import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class NetTest {

	public static void main(String[] args) throws Exception { 
		SelectorGroup group = new SelectorGroup();
		
		MessageClient client = new MessageClient("127.0.0.1:15555", group);
		Message req = new Message();
		req.setBody("test");
		try{
			client.send(req);
		}catch(IOException e){
			System.err.println(e);
		}
		//client.connectAsync(); 
		Thread.sleep(100);
		client.close();
		
		group.close(); 
	}

}
