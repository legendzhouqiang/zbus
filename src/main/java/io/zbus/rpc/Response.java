package io.zbus.rpc;

import java.util.Map;

public class Response {
	public String id;
	
	public Object result;
	public Object error;  
	
	public Map<String, Object> properties;  
}
