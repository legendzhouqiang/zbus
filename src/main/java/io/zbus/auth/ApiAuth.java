package io.zbus.auth;

import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import io.zbus.kit.CryptoKit;
import io.zbus.kit.JsonKit;

public interface ApiAuth {  
	public static final ApiAuth ALLOW_ALL = (request)->{ return new AuthResult(true); };
	public static final ApiAuth DENY_ANY = (request)->{ return new AuthResult(false); };
	
	public static final String APIKEY = "apiKey";
	public static final String SIGNATURE = "signature";  
	
	public static String signature(Map<String, Object> request, String apiKey, String secret) { 
		//Map应该按Key排序好，TreeMap已经排序好, TODO value如果是Map继续排序
    	JSONObject sorted = new JSONObject(true);
    	for(Entry<String, Object> e : request.entrySet()) {
    		sorted.put(e.getKey(), e.getValue());
    	}
    	sorted.put("apiKey", apiKey);
		String message = JsonKit.toJSONString(sorted);
		String sign = CryptoKit.hmacSign(message, secret); 
		return sign;
    }
	
	public static void setSignature(Map<String, Object> request, String apiKey, String secret) { 
		String sign = signature(request, apiKey, secret);
		request.put("apiKey", apiKey); 
		request.put("signature", sign);
    } 
	
	
	AuthResult auth(Map<String, Object> request); 
}
