package io.zbus.mq.model;

import java.util.HashSet;
import java.util.Set;

public class Subscription {
	public Set<String> topics = new HashSet<>();
	public String mq;
	public String channel;
	public String clientId; 
	public Integer window;
	public boolean isWebsocket = true;
}
