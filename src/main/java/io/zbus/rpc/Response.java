package io.zbus.rpc;

public class Response {
	public Object result;
	public Object error;
	
	public String channelId;  //Server populated: from socket id
	public String messageId;
}
