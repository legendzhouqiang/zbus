package org.remoting.callback;

import java.io.IOException;

import org.znet.Session;

 
public interface ErrorCallback { 
	public void onError(IOException e, Session sess) throws IOException;   
}
