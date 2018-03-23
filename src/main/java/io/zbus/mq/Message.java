package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

public class Message {
	public String command;  
	public Map<String, Object> headers = new HashMap<>();
	public Object body;
}
