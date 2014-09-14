package org.zbus;

import java.util.concurrent.atomic.AtomicInteger;

import org.zbus.client.Producer; 
import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.ticket.ResultCallback;

public class ProducerWithClient {
	public static void main(String[] args) throws Exception {   
		//1) 创建到ZbusServer的链接
		final RemotingClient client = new RemotingClient("127.0.0.1:15555"); 
		
		//2) 包装为生产者，client生命周期不受Producer控制，因此Producer是个轻量级对象
		Producer producer = new Producer(client, "MyMQ");  
		Message msg = new Message();   
		msg.setBody("hello world"); 
		long start = System.currentTimeMillis();
		int count = 10;
		final AtomicInteger counter = new AtomicInteger();
		for(int i=0;i<count;i++){
			producer.send(msg, new ResultCallback() { 
				@Override
				public void onCompleted(Message result) {  
					int value = counter.incrementAndGet();
					if(value % 10000 == 0){
						System.out.println(value); 
					}
				}
			}); 
		}
		long end = System.currentTimeMillis();
		System.out.println(count*1000.0/(end-start));
		System.out.println("done"); 
	} 
}
