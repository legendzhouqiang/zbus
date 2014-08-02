package org.remoting.callback;

import java.io.IOException;

import org.remoting.Message;
import org.znet.Session;

 
public interface MessageCallback { 
	public void onMessage(Message msg, Session sess) throws IOException;   
}
