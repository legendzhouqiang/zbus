package io.zbus.mq.model;

import java.util.HashMap;
 

public class MqInfo extends HashMap<String, Object> { 
	private static final long serialVersionUID = 8802567360098590561L;  
	
	public String getName() {
		return (String)get("name");
	}
	
	public void setName(String value) {
		put("name", value);
	} 
}
