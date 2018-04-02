package io.zbus.mq;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson.JSONObject;

import io.zbus.mq.subscription.Subscription;
import io.zbus.net.ServerAdaptor;
import io.zbus.net.Session;
import io.zbus.net.http.HttpMessage;

public class MqServerAdaptor extends ServerAdaptor {

	Map<String, Subscription> subscriptionTable = new ConcurrentHashMap<>();

	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		if (msg instanceof byte[]) {
			handleWebsocketMessage((byte[]) msg, sess);
		} else if (msg instanceof HttpMessage) {
			handleHttpMessage((HttpMessage) msg, sess);
		}
	}

	protected void handleWebsocketMessage(byte[] msg, Session sess) throws IOException {
		JSONObject json = JSONObject.parseObject(new String(msg));
		String cmd = json.getString("cmd");
		if (cmd == null) {
			JSONObject res = new JSONObject();
			res.put("status", 400);
			res.put("data", "missing command");
			res.put("id", json.getString("id"));

			sess.write(JSONObject.toJSONBytes(res));
			return;
		}

		if (cmd.equals("sub")) {
			handleSubMessage(json, sess);
			return;
		}
		
		if (cmd.equals("pub")) {
			handlePubMessage(json, sess);
			return;
		} 
	}

	protected void handlePubMessage(JSONObject json, Session sess) throws IOException {
		String topic = json.getString("topic");
		for(Entry<String, Subscription> e : subscriptionTable.entrySet()) {
			Subscription sub = e.getValue();
			if(sub.topics.contains(topic)) {
				Session matched = sessionTable.get(sub.clientId);
				if(matched != null) { 
					json.remove("cmd");
					matched.write(JSONObject.toJSONBytes(json));
				}
			}
		} 
	}
	
	protected void handleSubMessage(JSONObject json, Session sess) throws IOException {
		Subscription sub = subscriptionTable.get(sess.id());
		if(sub == null) {
			sub = new Subscription();
			sub.clientId = sess.id();
			subscriptionTable.put(sub.clientId, sub);
		}
		sub.topics.clear();
		sub.topics.add(json.getString("topic"));
	}

	protected void handleHttpMessage(HttpMessage msg, Session sess) throws IOException {

	}
}
