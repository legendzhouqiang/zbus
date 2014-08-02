package org.remoting.callback;

import java.io.IOException;

import org.znet.Session;

 
public interface ConnectedCallback { 
	public void onConnected(Session sess) throws IOException;   
}
