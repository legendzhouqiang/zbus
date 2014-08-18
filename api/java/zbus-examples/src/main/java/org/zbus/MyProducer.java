package org.zbus;

import org.zbus.client.Producer; 
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.ticket.ResultCallback;

public class MyProducer {
	public static void main(String[] args) throws Exception {   
		//1) 创建到ZbusServer的链接
		final RemotingClient client = new RemotingClient("127.0.0.1:15555"); 
		
		//2) 包装为生产者，client生命周期不受Producer控制，因此Producer是个轻量级对象
		Producer producer = new Producer(client, "MyMQ");  
		for(int i=0;i<100;i++){
			Message msg = new Message();  
			msg.setBody("hello world"); 
			producer.send(msg, new ResultCallback() { 
				@Override
				public void onCompleted(Message result) {  
					System.out.println(result); 
				}
			});  
		}
		//client.close();
	} 
}
