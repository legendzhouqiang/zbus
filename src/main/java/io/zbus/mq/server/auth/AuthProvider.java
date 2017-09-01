package io.zbus.mq.server.auth;

import io.zbus.mq.Message;

public interface AuthProvider {
	boolean auth(Message message); 
}
