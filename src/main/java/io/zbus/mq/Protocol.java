package io.zbus.mq;


public interface Protocol {  
	public static final String CMD    = "cmd";
	
	//Commands
	public static final String PUB    = "pub";
	public static final String SUB    = "sub";  
	public static final String CREATE = "create";  //Create or Update
	public static final String REMOVE = "remove";
	public static final String PING   = "ping";
	
	//Parameters
	public static final String MQ             = "mq";  
	public static final String CHANNEL        = "channel";  
	public static final String TOPIC          = "topic";   
	public static final String SENDER         = "sender"; 
	public static final String MQ_TYPE        = "mqType";  
	public static final String MQ_MASK        = "mqMask";
	public static final String CHANNEL_MASK   = "channelMask";
	public static final String CHANNEL_OFFSET = "channelOffset";
	public static final String WINDOW         = "window";
	public static final String ACK            = "ack";
	
	public static final String ID     = "id";   
	public static final String DATA   = "data";   
	public static final String STATUS = "status"; 
	
	public static final String MQ_TYPE_MEM = "mem";  
	public static final String MQ_TYPE_DISK = "disk";  
	public static final String MQ_TYPE_DB = "db";  
	
}
