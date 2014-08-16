package org.zbus.remoting.ticket;

import org.zbus.remoting.Message;

 
public interface ResultCallback { 
	public void onCompleted(Message result);  
}
