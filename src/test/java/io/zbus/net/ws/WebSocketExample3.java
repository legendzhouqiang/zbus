package io.zbus.net.ws;

import com.alibaba.fastjson.JSONObject;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.net.EventLoop;
import io.zbus.net.http.WebsocketClient;

public class WebSocketExample3 {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();
		String address = "wss://api.huobipro.com/ws";

		WebsocketClient ws = new WebsocketClient(address, loop);

		ws.onMessage = msg -> {
			System.out.println(msg); 
		};

		ws.onOpen = () -> { 
			JSONObject req = new JSONObject();
			req.put("sub", "market.btcusdt.kline.1min");
			req.put("id", StrKit.uuid());
			ws.sendMessage(JsonKit.toJSONString(req));
		};

		ws.connect();
	}
}
