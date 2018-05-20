package io.zbus.auth;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import io.zbus.kit.CryptoKit;

public class DefaultSign implements RequestSign {  
	public String calcSignature(Map<String, Object> request, String apiKey, String secret) {  
    	Map<String, Object> copy = new HashMap<>(request);
    	copy.put(APIKEY, apiKey);
    	String message = JSON.toJSONString(copy, SerializerFeature.MapSortField); //Sort map by key 
		String sign = CryptoKit.hmacSign(message, secret); 
		return sign;
    }
	
	public void sign(Map<String, Object> request, String apiKey, String secret) { 
		String sign = calcSignature(request, apiKey, secret);
		request.put(APIKEY, apiKey); 
		request.put(SIGNATURE, sign);
    }   
}
