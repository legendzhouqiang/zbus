package org.remoting;

import java.io.IOException;

import org.remoting.Message;
import org.znet.Session;

 
public interface MessageHandler { 
	public void handleMessage(Message msg, Session sess) throws IOException;   
}
