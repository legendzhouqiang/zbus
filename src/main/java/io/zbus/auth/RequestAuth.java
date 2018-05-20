package io.zbus.auth;

import java.util.Map;


/**
 * Do authentication check for request of JSON type
 * 
 * @author leiming.hong
 *
 */
public interface RequestAuth {     
	public static final String APIKEY = "apiKey";
	public static final String SIGNATURE = "signature";  
	
	public static final RequestAuth ALLOW_ALL = (request)->{ return new AuthResult(true); };
	public static final RequestAuth DENY_ANY = (request)->{ return new AuthResult(false); };
	
	/**
	 * Do authentication check for request of JSON type
	 * 
	 * @param request JSON typed request object
	 * @return authentication result, success set to true of false with failure message
	 */
	AuthResult auth(Map<String, Object> request); 
}
