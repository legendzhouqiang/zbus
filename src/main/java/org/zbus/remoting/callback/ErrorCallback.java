package org.zbus.remoting.callback;

import java.io.IOException;

import org.zbus.remoting.znet.Session;

 
public interface ErrorCallback { 
	public void onError(IOException e, Session sess) throws IOException;   
}
