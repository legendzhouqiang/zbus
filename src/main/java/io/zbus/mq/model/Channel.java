package io.zbus.mq.model;

public class Channel implements Cloneable {   
	public final String name;
	public Long offset; 
	public Integer mask; 
	
	public Channel(String name, Long offset) {
		this.name = name;
		this.offset = offset;
	}
	public Channel(String name) {
		this(name, null);
	}
	
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
