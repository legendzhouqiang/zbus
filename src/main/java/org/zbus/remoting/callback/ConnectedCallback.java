package org.zbus.remoting.callback;

import java.io.IOException;

import org.zbus.remoting.nio.Session;

 
public interface ConnectedCallback { 
	public void onConnected(Session sess) throws IOException;   
}
