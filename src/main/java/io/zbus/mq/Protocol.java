package io.zbus.mq;

/**
 * 
 * JSON format
 * 
 * Request message format
 * 
 * cmd:     pub|sub|create|remove|ping
 * mq:      <mqName>
 * channel: <channelName>  Optional
 * id:      <messageId>
 * data:    <messageBody>  business related data
 * 
 * 
 * Response message format(Compared to Request)
 * cmd removed
 * status added 
 * 
 * status:  200|400|404|500 ... similar to HTTP status 
 * 
 * @author leiming.hong
 *
 */ 
public interface Protocol {  
	public static final String CMD    = "cmd";
	
	//Commands
	public static final String PUB    = "pub";
	public static final String SUB    = "sub";  
	public static final String CREATE = "create";  //Create or Update
	public static final String REMOVE = "remove";
	public static final String PING   = "ping";
	
	//Parameters
	public static final String MQ     = "mq";  
	public static final String CHANNEL= "channel";  
	public static final String TOPIC  = "topic";   
	public static final String MQ_TYPE= "mqType";  
	public static final String WINDOW = "window";
	
	//Response format
	public static final String DATA   = "data";  
	public static final String ID     = "id";  
	public static final String STATUS = "status"; 
	
}
