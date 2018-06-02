package io.zbus.mq;


public interface Protocol {  
	//Parameter keys
	public static final String CMD    = "cmd";     // Request message command
	public static final String STATUS = "status";  // Response message status
	public static final String ID     = "id";      // Message ID
	public static final String DATA   = "data";    // Message body 
	
	//Command values(key=cmd)
	public static final String PUB    = "pub";
	public static final String SUB    = "sub";  
	public static final String CREATE = "create";  //Create or Update
	public static final String REMOVE = "remove";
	public static final String PING   = "ping"; 
	
	//Parameter keys(for commands)
	public static final String MQ             = "mq";  
	public static final String CHANNEL        = "channel";  
	public static final String TOPIC          = "topic";   
	public static final String OFFSET         = "offset";
	public static final String SENDER         = "sender"; 
	public static final String MQ_TYPE        = "mqType";  
	public static final String MQ_MASK        = "mqMask";
	public static final String CHANNEL_MASK   = "channelMask"; 
	public static final String WINDOW         = "window";
	public static final String ACK            = "ack"; 
	
	//Parameter mqType values
	public static final String MEMORY  = "memory";  
	public static final String DISK    = "disk";  
	public static final String DB      = "db";  
	
}
