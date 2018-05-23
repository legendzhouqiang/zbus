package io.zbus.mq.model;

public class Channel implements Cloneable {   
	public String name;
	public Long offset; 
	public Long mask; 
	
	@Override
	public Channel clone() { 
		try {
			Channel channel = (Channel)super.clone(); 
			return channel;
		} catch (CloneNotSupportedException e) { 
			throw new IllegalStateException(e.getMessage(), e.getCause());
		}   
	}
}
