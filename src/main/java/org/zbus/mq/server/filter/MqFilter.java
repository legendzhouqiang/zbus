package org.zbus.mq.server.filter;

import java.io.Closeable;

import org.zbus.net.http.Message;

public interface MqFilter extends Closeable {
	void removeKey(String mq, String key);
	boolean permit(Message msg);
}