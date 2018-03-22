package io.zbus.net;

public interface MessageHandler<T> { 
	void handle(T e) throws Exception;   
}