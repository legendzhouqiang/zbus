package org.zbus;

import java.util.concurrent.atomic.AtomicInteger;

import org.remoting.Message;
import org.remoting.RemotingClient;
import org.remoting.ticket.ResultCallback;
import org.zbus.client.Producer;
import org.zbus.common.MessageMode;


public class PubWithClient {

	public static void main(String[] args) throws Exception {   
		//1) 创建到ZbusServer的链接
		final RemotingClient client = new RemotingClient("127.0.0.1", 15555); 
		
		//2) 包装为生产者，client生命周期不受Producer控制，因此Producer是个轻量级对象
		Producer producer = new Producer(client, "MySub", MessageMode.PubSub); 
		final int count = 100; //重复发送场景
		final AtomicInteger idx = new AtomicInteger();
		for(int i=0;i<count;i++){    
			Message msg = new Message();  
			msg.setTopic("qhee"); //设定消息主题
			msg.setBody("hello world, %04d", i); 
			producer.send(msg, new ResultCallback() { 
				@Override
				public void onCompleted(Message result) {  
					System.out.println(idx.incrementAndGet()+":"+result); 
				}
			}); 
		}  
	}

}
