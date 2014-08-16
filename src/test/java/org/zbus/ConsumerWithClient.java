package org.zbus;

import java.io.IOException;

import org.zbus.client.Consumer; 
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;

public class ConsumerWithClient { 
	public static void main(String[] args) throws IOException{  
		//1) 创建到ZbusServer的链接
		final RemotingClient client = new RemotingClient("127.0.0.1", 15555);
		//2) 包装为消费者，client生命周期不受Consumer控制，因此Consumer是个轻量级对象
		Consumer consumer = new Consumer(client, "MyMQ");    
		while(true){
			Message msg = consumer.recv(10000);
			if(msg == null) continue; 
			System.out.println(msg); 
		} 
	} 
}
