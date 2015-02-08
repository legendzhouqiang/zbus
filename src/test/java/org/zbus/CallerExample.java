package org.zbus;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.Caller;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.broker.SingleBroker;
import org.zbus.remoting.Message;
import org.zbus.remoting.ticket.ResultCallback;

public class CallerExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代表
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		
		//2) 创建生产者
		Caller caller = new Caller(broker, "PingPong");
		
		Message msg = new Message();
		msg.setBody("hello world");
		
		caller.invokeAsync(msg, new ResultCallback() {
			@Override
			public void onCompleted(Message result) {
				System.out.println(result);
			}
		});
		
	} 
}
