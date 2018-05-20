package io.zbus.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.alibaba.fastjson.JSONObject;

import io.zbus.kit.JsonKit;

public class SignAuthExample {
	public static void main(String[] args) {
		ApiKeyProvider apiKeyProvider = new XmlApiKeyProvider("rpc/auth.xml");
		RequestAuth auth = new DefaultAuth(apiKeyProvider);
		
		RequestSign sign = new DefaultSign();
		
		Map<String, Object> req = new HashMap<>();
		for(int i=0;i<10;i++) {
			req.put("key"+i, new Random().nextInt());
		}
		
		Map<String, Object> f = new HashMap<>();
		for(int i=0;i<10;i++) {
			f.put("key"+i, new Random().nextInt());
		}
		req.put("composit", f);
		
		String apiKey = "2ba912a8-4a8d-49d2-1a22-198fd285cb06";
		String secret = "461277322-943d-4b2f-b9b6-3f860d746ffd";
		
		sign.sign(req, apiKey, secret);
		
		String wired = JsonKit.toJSONString(req);
		System.out.println(wired);
		JSONObject req2 = JsonKit.parseObject(wired, JSONObject.class);
		AuthResult res = auth.auth(req2);
		
		System.out.println(res.success); 
	}
}
