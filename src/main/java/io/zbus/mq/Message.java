package io.zbus.mq;

import java.util.TreeMap;

public class Message extends TreeMap<String, Object> {     
	private static final long serialVersionUID = -2119891220813658947L;
 
	public String getApiKey() { 
		return get("apiKey");
	}
	public void setApiKey(String value) {
		put("apiKey", value);
	} 
	
	public String getSignature() { 
		return get("signature");
	}
	
	public void setSignature(String value) {
		put("signature", value);
	}  
	
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T)super.get(key);
	}
}