package io.zbus.rpc;

public class Request {
	public String method;
	public Object[] params;
	 
	public String module; 
	public String[] paramTypes; 
	public String version;
	
	public String channelId;  //Server populated: from socket id
	public String messageId;
}