package org.zbus.net;

import java.io.IOException;

import org.zbus.net.core.Session;

 
public interface MsgHandler<T> { 
	public void handle(T msg, Session sess) throws IOException;   
}
