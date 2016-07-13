package org.zbus.examples.service;

import org.zbus.broker.Broker;
import org.zbus.broker.ZbusBroker;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.mq.MqInvoker;

public class MyServiceClientExample {

	public static void main(String[] args) throws Exception { 
		
		Broker broker = new ZbusBroker("127.0.0.1:15555");   
		MessageInvoker invoker = new MqInvoker(broker, "MyService");
		
		Message req = new Message();
		req.setRequestPath("/api/report/articles");
		req.setRequestParam("filter", "{where:{id:{lt:3}}}");
		req.setRequestParam("invite_code", "1"); 
		
		Message resp = invoker.invokeSync(req);
		System.out.println(resp);
		
		broker.close(); 
	}

}
