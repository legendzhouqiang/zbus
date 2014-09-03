package org.zbus.remoting;

import java.io.IOException;

import org.zbus.remoting.Message;
import org.zbus.remoting.nio.Session;

 
public interface MessageHandler { 
	public void handleMessage(Message msg, Session sess) throws IOException;   
}
