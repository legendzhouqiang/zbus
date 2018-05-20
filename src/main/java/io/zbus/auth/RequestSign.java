package io.zbus.auth;

import java.util.Map;

public interface RequestSign {    
	public static final String APIKEY = "apiKey";
	public static final String SIGNATURE = "signature";  
	
	String calcSignature(Map<String, Object> request, String apiKey, String secret); 
	void sign(Map<String, Object> request, String apiKey, String secret);
}
