package org.zbus;

import java.io.IOException;

import org.zbus.client.Broker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.service.Caller;
import org.zbus.remoting.Message;
import org.zbus.remoting.ticket.ResultCallback;

public class CallerExample {
	public static void main(String[] args) throws IOException{  
		//1）创建Broker代理【重量级对象，需要释放】
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		final Broker broker = new SingleBroker(config);
		
		//2) 【轻量级对象，不需要释放，随便使用】
		Caller caller = new Caller(broker, "MyService");
		
		Message msg = new Message();
		msg.setBody("hello world");
		
		caller.invokeAsync(msg, new ResultCallback() {
			@Override
			public void onCompleted(Message result) {
				System.out.println(result);
				
				//销毁Broker，注意，这里仅仅是为了方便应用退出
				broker.destroy();
			}
		});
		
	} 
}
