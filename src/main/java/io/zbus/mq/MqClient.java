package io.zbus.mq;

import java.util.Map;

import com.alibaba.fastjson.JSON;

import io.zbus.net.EventLoop;
import io.zbus.net.http.WebsocketClient;

public class MqClient extends WebsocketClient{

	public MqClient(String address, EventLoop loop) {
		super(address, loop); 
	}
	 
	public void sendMessage(Map<String, Object> req) { 
		super.sendMessage(JSON.toJSONString(req));
	}  
}
