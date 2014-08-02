package org.zbus.agent;

import org.remoting.Message;
import org.remoting.ticket.ResultCallback;
import org.zbus.client.Producer;
import org.zbus.client.ha.AgentConfig;
import org.zbus.client.ha.ClientAgent;


public class ProducerWithAgent {

	public static void main(String[] args) throws Exception {   
		AgentConfig config = new AgentConfig();
		config.setTrackServerList("127.0.0.1:16666;127.0.0.1:16667");
		ClientAgent agent = new ClientAgent(config);
		  
		//2) 包装为生产者，client生命周期不受Producer控制，因此Producer是个轻量级对象
		Producer producer = new Producer(agent, "MyMQ");
		
		final int count = 500; //重复发送场景
		long start = System.currentTimeMillis();
		for(int i=0;i<count;i++){   
			
			//组装消息，消息格式主要由KV头部+Body组成
			Message msg = new Message(); 
			msg.setHead("cookie", "test=ok");
			msg.setBody("hello world");
			
			producer.send(msg, new ResultCallback() { 
				@Override
				public void onCompleted(Message result) {
					//System.out.println(result); 
					
				}
			});
		}  
		long end = System.currentTimeMillis();
		double QPS = (1000.0*count)/(end-start);
		System.out.println(QPS+"--done--");
	}

}
