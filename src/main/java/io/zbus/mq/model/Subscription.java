package io.zbus.mq.model;

import java.util.ArrayList;
import java.util.List;

public class Subscription {
	public List<String> topics = new ArrayList<>();
	public String mq;
	public String channel;
	public String clientId; 
	public Integer window;
	public boolean isWebsocket = true;
}
