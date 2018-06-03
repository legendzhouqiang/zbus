package io.zbus.mq;


public interface Protocol {  
	//Parameter keys(Message main key-value pairs)
	public static final String CMD       = "cmd";       // Request message command
	public static final String STATUS    = "status";    // Response message status
	public static final String ID        = "id";        // Message ID
	public static final String BODY      = "body";      // Message body 
	public static final String API_KEY   = "apiKey";    // Authentication public Key
	public static final String SIGNATURE = "signature"; // Authentication signature generated
	
	//Command values(key=cmd)
	public static final String PUB    = "pub";      //Publish message
	public static final String SUB    = "sub";      //Subscribe message stream
	public static final String TAKE   = "take";     //One-time read message from MQ 
	public static final String CREATE = "create";   //Create or Update
	public static final String REMOVE = "remove";   //Remove MQ/Channel
	public static final String QUERY  = "query";    //Query MQ/Channel
	public static final String PING   = "ping";     //Heartbeat ping
	
	//Parameter keys(for commands)
	public static final String MQ             = "mq";  
	public static final String CHANNEL        = "channel";  
	public static final String TOPIC          = "topic";     //Topic filter message on Channel
	public static final String OFFSET         = "offset";
	public static final String CHECKSUM       = "checksum";  //Offset checksum
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
