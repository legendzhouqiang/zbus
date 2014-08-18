package org.zbus;

import org.zbus.client.Producer;
import org.zbus.common.MessageMode;
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.ticket.ResultCallback;


public class MyPub {

	public static void main(String[] args) throws Exception {  
		final RemotingClient client = new RemotingClient("127.0.0.1", 15555); 
		//指定消息模式为发布订阅
		Producer producer = new Producer(client, "MySub", MessageMode.PubSub); 
		Message msg = new Message();  
		msg.setTopic("qhee"); //设定消息主题
		msg.setBody("hello world"); 
		producer.send(msg, new ResultCallback() { 
			@Override
			public void onCompleted(Message result) {  
				System.out.println(result); 
			}
		}); 
	}  
}
