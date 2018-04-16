package io.zbus.mq;

import com.alibaba.fastjson.JSONObject;

import io.zbus.mq.model.ChannelInfo;
import io.zbus.mq.model.MessageQueueInfo;
import io.zbus.net.EventLoop;

public class Create { 
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();  
		MqClient ws = new MqClient("localhost:15555", loop);

		ws.onMessage = msg -> {
			System.out.println(msg); 
		};

		ws.onOpen = () -> { 
			JSONObject req = new JSONObject();
			req.put("cmd", "create"); 
			
			MessageQueueInfo mqInfo = new MessageQueueInfo();
			mqInfo.setName("MyMQ");           
			
			ChannelInfo channelInfo = new ChannelInfo();
			channelInfo.setName("MyChannel"); 
			
			req.put("mq", mqInfo);            // { name: "MyMQ" }
			req.put("channel", channelInfo);  // { name: "MyChannel" }
			 
			ws.sendMessage(req);
		};

		ws.connect();
	} 
}
