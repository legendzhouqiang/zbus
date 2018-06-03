package io.zbus.auth;

import java.util.HashMap;
import java.util.Map;

public class DefaultAuth implements RequestAuth {
	private ApiKeyProvider apiKeyProvider;
	private RequestSign requestSign = new DefaultSign();

	public DefaultAuth(ApiKeyProvider apiKeyProvider) {
		this.apiKeyProvider = apiKeyProvider;
	}
	
	public void setRequestSign(RequestSign requestSign) {
		this.requestSign = requestSign;
	}
	
	@Override
	public AuthResult auth(Map<String, Object> request) { 
		String apiKey = (String)request.get(APIKEY);
		if(apiKey == null) return new AuthResult(false, "missing apiKey in request");
		String sign = (String)request.get(SIGNATURE);
		if(sign == null) return new AuthResult(false, "missing signature in request");
		
		if(!apiKeyProvider.apiKeyExists(apiKey)) return new AuthResult(false, "apiKey not exists");
		String secretKey = apiKeyProvider.secretKey(apiKey);
		if(secretKey == null) return new AuthResult(false, "secretKey not exists");
		
		Map<String, Object> copy = new HashMap<>(request);
		copy.remove(SIGNATURE);
		
		String sign2 = requestSign.calcSignature(copy, apiKey, secretKey);
		if(sign.equals(sign2)) {
			return new AuthResult(true);
		} else {
			return new AuthResult(false, "signature unmatched");
		}
	} 
}
