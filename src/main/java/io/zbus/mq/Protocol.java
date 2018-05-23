package io.zbus.mq;

/**
 * 
 * JSON format
 * 
 * Request message format
 * 
 * cmd:       pub|sub|create|remove|ping
 * mq:        <mqName>
 * channel:   <channelName> 
 * id:        <messageId>
 * data:      <messageBody>  business related data
 * apiKey:    <apiKey>
 * secretKey: <secretKey>
 * 
 * Response message format(Compared to Request)
 * cmd removed
 * status added 
 * 
 * status:  200|400|404|500 ... similar to HTTP status 
 * 
 * 1.1) Create MQ
 * [R] cmd: create
 * [R] mq:  <mqName> 
 * [O] mqType: mem|disk|db (default to mem)
 * [O] mqMask: <Long> MQ's mask
 * 
 * 1.2) Create Channel
 * [R] cmd: create
 * [R] mq: <mqName>
 * [R] channel: <channelName> 
 * [O] channelOffset: <longOffset> (default to the end of mq)
 * [O] channelMask: <Long> channel's Mask
 * 
 * 2.1) Remove MQ
 * [R] cmd: remove
 * [R] mq: <mqName>
 * 
 * 2.2) Remove Channel
 * [R] cmd: remove
 * [R] mq: <mqName>
 * [R] channel: <channelName>
 * 
 * 3) Publish
 * [R] cmd: pub
 * [R] mq: <mqName>
 * [R] data: <data>
 * 
 * 4) Subscribe
 * [R] cmd: sub
 * [R] mq: <mqName>
 * [R] channel: <channelName>
 * [O] window: <integer> 
 * 
 * 5) Ping
 * cmd: ping
 * 
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
	public static final String MQ           = "mq";  
	public static final String CHANNEL      = "channel";  
	public static final String TOPIC        = "topic";   
	public static final String MQ_TYPE      = "mqType";  
	public static final String MQ_MASK      = "mqMask";
	public static final String CHANNEL_MASK = "channelMask";
	public static final String WINDOW       = "window";
	
	//Response format
	public static final String DATA   = "data";  
	public static final String ID     = "id";  
	public static final String STATUS = "status"; 
	
}
