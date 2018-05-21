package io.zbus.mq;
 
public interface Protocol {  
	public static final String CMD    = "cmd";
	
	//Commands
	public static final String PUB    = "pub";
	public static final String SUB    = "sub";  
	public static final String CREATE = "create"; 
	public static final String UPDATE = "update"; 
	public static final String REMOVE = "remove";
	public static final String PING   = "ping";
	
	//Parameters
	public static final String MQ     = "mq";  
	public static final String TOPIC  = "topic";  
	public static final String CHANNEL= "channel";  
	
	//Response format
	public static final String DATA   = "data";  
	public static final String ID     = "id";  
	public static final String STATUS = "status"; 
	
}
