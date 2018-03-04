package io.zbus.net;

import java.io.IOException;

public interface SessionMessageHandler<T> { 
	void handle(T msg, Session session) throws IOException;   
}