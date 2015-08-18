package org.zbus.net;

import java.io.IOException;

import org.zbus.net.core.Session;

 
public interface ErrorHandler { 
	public void onError(IOException e, Session sess) throws IOException;   
}
