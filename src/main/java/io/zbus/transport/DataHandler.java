package io.zbus.transport;

public interface DataHandler<T> { 
	void handle(T data) throws Exception;   
}