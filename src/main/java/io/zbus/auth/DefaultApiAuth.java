package io.zbus.auth;

import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSONObject;

public class DefaultApiAuth implements ApiAuth {
	private ApiKeyProvider apiKeyProvider;

	public DefaultApiAuth(ApiKeyProvider apiKeyProvider) {
		this.apiKeyProvider = apiKeyProvider;
	}
	
	@Override
	public AuthResult auth(Map<String, Object> request) { 
		String apiKey = (String)request.get(APIKEY);
		if(apiKey == null) return new AuthResult(false, "missing apiKey in request");
		String sign = (String)request.get(SIGNATURE);
		if(sign == null) return new AuthResult(false);
		
		if(!apiKeyProvider.apiKeyExists(apiKey)) return new AuthResult(false, "apiKey not exists");
		String secretKey = apiKeyProvider.secretKey(apiKey);
		if(secretKey == null) return new AuthResult(false, "secretKey not exists");
		
		JSONObject sorted = new JSONObject(true);
		for(Entry<String, Object> e : request.entrySet()) {
			if(SIGNATURE.equals(e.getKey())) continue; 
			sorted.put(e.getKey(), e.getValue());
		}
		
		String sign2 = ApiAuth.signature(sorted, apiKey, secretKey);
		if(sign.equals(sign2)) {
			return new AuthResult(true);
		} else {
			return new AuthResult(false, "signature unmatched");
		}
	} 
}
