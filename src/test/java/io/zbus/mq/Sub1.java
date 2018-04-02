package io.zbus.mq;

import com.alibaba.fastjson.JSONObject;

import io.zbus.kit.JsonKit;
import io.zbus.net.EventLoop;
import io.zbus.net.http.WebsocketClient;

public class Sub1 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();
		String address = "ws://localhost";

		WebsocketClient ws = new WebsocketClient(address, loop);

		ws.onMessage = msg -> {
			System.out.println(msg); 
		};

		ws.onOpen = () -> { 
			JSONObject req = new JSONObject();
			req.put("cmd", "sub"); 
			req.put("topic", "/abc"); 
			 
			ws.sendMessage(JsonKit.toJSONString(req));
		};

		ws.connect();
	} 
}
