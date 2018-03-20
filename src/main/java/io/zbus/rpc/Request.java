package io.zbus.rpc;

public class Request {
	public String method;
	public Object[] params;
	public String[] paramTypes; 
	
	public String app;    //app + module + method
	public String module;   
	
	public String channelId;  //Server populated: from socket id
	public String messageId;
}