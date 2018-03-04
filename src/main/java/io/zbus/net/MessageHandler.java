package io.zbus.net;

import java.io.IOException;

public interface MessageHandler<T> { 
	void handle(T e) throws IOException;   
}