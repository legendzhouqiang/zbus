package io.zbus.mq;

import java.util.Map;

import com.alibaba.fastjson.JSON;

import io.zbus.net.EventLoop;
import io.zbus.net.http.WebsocketClient;

public class MqClient extends WebsocketClient{

	public MqClient(String address, EventLoop loop) {
		super(normalizeAddress(address), loop); 
	}
	
	private static String normalizeAddress(String address) {
		if(!address.startsWith("ws://") && !address.startsWith("wss://")) {
			address = "ws://" + address;
		}
		return address;
	} 
	 
	public void sendMessage(Map<String, Object> req) { 
		super.sendMessage(JSON.toJSONString(req));
	}   
	
}
