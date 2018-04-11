package io.zbus.mq.model;

import java.util.ArrayList;
import java.util.List;

public class Subscription {
	public List<String> topics = new ArrayList<>();
	public String clientId; 
	public Integer window;
}
