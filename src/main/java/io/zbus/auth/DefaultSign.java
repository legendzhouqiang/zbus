package io.zbus.auth;

import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

import io.zbus.kit.CryptoKit;
import io.zbus.kit.JsonKit;

public class DefaultSign implements RequestSign {  
	public String calcSignature(Map<String, Object> request, String apiKey, String secret) { 
		//Map应该按Key排序好，TreeMap已经排序好, TODO value如果是Map继续排序
    	JSONObject sorted = new JSONObject(true);
    	for(Entry<String, Object> e : request.entrySet()) {
    		sorted.put(e.getKey(), e.getValue());
    	}
    	sorted.put(APIKEY, apiKey);
		String message = JsonKit.toJSONString(sorted);
		String sign = CryptoKit.hmacSign(message, secret); 
		return sign;
    }
	
	public void sign(Map<String, Object> request, String apiKey, String secret) { 
		String sign = calcSignature(request, apiKey, secret);
		request.put(APIKEY, apiKey); 
		request.put(SIGNATURE, sign);
    }   
}
