package org.zbus.examples.gateway;

import org.zbus.mq.Protocol;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;
 

public class GatewayClient {

	public static void main(String[] args) throws Exception { 
		SelectorGroup group = new SelectorGroup();
		MessageClient client = new MessageClient("127.0.0.1:15555", group);
		
		Message req = new Message();
		req.setMq("Gateway");
		req.setCmd(Protocol.Produce);
		req.setAck(false);
		req.setBody("test");
		
		
		Message res = client.invokeSync(req);
		System.out.println(res);
		
		client.close();
		group.close(); 
		
		
	}

}
