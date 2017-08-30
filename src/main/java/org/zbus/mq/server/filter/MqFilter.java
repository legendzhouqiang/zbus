package org.zbus.mq.server.filter;

import java.io.Closeable;

import org.zbus.net.http.Message;

public interface MqFilter extends Closeable { 
	boolean permit(Message msg); 
	int addKey(String mq, String group, String key);
	int removeKey(String mq, String group, String key);
	
}