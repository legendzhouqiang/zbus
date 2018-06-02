package io.zbus.rpc;

import java.util.HashMap;

public class Response extends HashMap<String, Object>  { 
	private static final long serialVersionUID = 3189028874502200908L; 
	
	public Integer getStatus() {
		return get("status");
	}
	
	public void setStatus(Integer value) {
		put("status", value);
	} 
	
	public Object getBody() {
		return get("body");
	}
	
	public void setBody(Object value) {
		put("body", value);
	}    
	
	public String getId() { 
		return get("id");
	}
	public void setId(String value) {
		put("id", value);
	} 
	
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T)super.get(key);
	}
}
