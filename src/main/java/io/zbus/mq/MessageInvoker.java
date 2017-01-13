package io.zbus.mq;

import java.io.IOException;

public interface MessageInvoker{   
	
	Message invokeSync(Message req, int timeout) throws IOException, InterruptedException; 
	
	Message invokeSync(Message req) throws IOException, InterruptedException; 
	
	void invokeAsync(Message req, MessageCallback callback) throws IOException;
}