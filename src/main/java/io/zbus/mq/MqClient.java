package io.zbus.mq;

import java.util.Map;

import com.alibaba.fastjson.JSON;

import io.zbus.transport.http.WebsocketClient; 

public class MqClient extends WebsocketClient{

	public MqClient(String address) { 
		super(address);
	} 
	 
	public void sendMessage(Map<String, Object> req) { 
		super.sendMessage(JSON.toJSONString(req));
	}   
	
}
