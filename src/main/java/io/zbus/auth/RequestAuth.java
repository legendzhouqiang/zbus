package io.zbus.auth;

import java.util.Map;

public interface RequestAuth {    
	public static final String APIKEY = "apiKey";
	public static final String SIGNATURE = "signature";  
	
	public static final RequestAuth ALLOW_ALL = (request)->{ return new AuthResult(true); };
	public static final RequestAuth DENY_ANY = (request)->{ return new AuthResult(false); };
	
	AuthResult auth(Map<String, Object> request); 
}
