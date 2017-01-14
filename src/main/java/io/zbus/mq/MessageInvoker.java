package io.zbus.mq;

import java.io.Closeable;
import java.io.IOException;

public interface MessageInvoker extends Closeable{   
	
	Message invokeSync(Message req, int timeout) throws IOException, InterruptedException;  
	
	void invokeAsync(Message req, MessageCallback callback) throws IOException;
}