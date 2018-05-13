package io.zbus.net;

public interface DataHandler<T> { 
	void handle(T data) throws Exception;   
}