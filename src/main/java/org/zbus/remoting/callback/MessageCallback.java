package org.zbus.remoting.callback;

import java.io.IOException;

import org.zbus.remoting.Message;
import org.zbus.remoting.nio.Session;

 
public interface MessageCallback { 
	public void onMessage(Message msg, Session sess) throws IOException;   
}
