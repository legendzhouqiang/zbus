package io.zbus.mq.model;

import java.util.HashMap;

public class Channel implements Cloneable {   
	public String name;
	public long offset; 
	
	public HashMap<String, Object> attributes = new HashMap<>();
	@Override
	public Channel clone() { 
		try {
			Channel channel = (Channel)super.clone();
			channel.attributes = new HashMap<>(channel.attributes);
			return channel;
		} catch (CloneNotSupportedException e) { 
			throw new IllegalStateException(e.getMessage(), e.getCause());
		}   
	}
}
