package io.zbus.mq.server;

import io.zbus.mq.Message;


public interface MessageLogger { 
	void log(Message message);  
}

