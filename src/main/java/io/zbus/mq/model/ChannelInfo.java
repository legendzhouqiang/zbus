package io.zbus.mq.model;

import java.util.HashMap;
 

public class ChannelInfo extends HashMap<String, Object> { 
	private static final long serialVersionUID = 8802567360098590561L;  
	
	public String getName() {
		return (String)get("name");
	}
	
	public void setName(String value) {
		put("name", value);
	} 
	
	public Long getOffset() {
		Object value = get("offset");
		if(value == null) return null;
		
		if(value instanceof Number) {
			return ((Number)value).longValue();
		}
		
		if(value instanceof String) {
			return Long.valueOf((String)value);
		}
		
		return null;
	}
	
	public void setOffset(Long value) {
		put("offset", value);
	}
	
	public Channel toChannel() {
		Channel channel = new Channel();
		channel.name = getName();
		channel.offset = getOffset();
		channel.attributes = new HashMap<>(this);
		channel.attributes.remove("name");
		channel.attributes.remove("offset");
		return channel;
	}
}
