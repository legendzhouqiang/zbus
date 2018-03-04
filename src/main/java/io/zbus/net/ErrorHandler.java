package io.zbus.net;

public interface ErrorHandler { 
	void handle(Throwable e) throws Exception;   
}