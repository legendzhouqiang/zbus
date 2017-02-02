package io.zbus.mq;

import java.util.HashMap;
import java.util.Map;

import io.zbus.mq.Protocol.ServerInfo;

public class TrackTable {
	public String publisher; //publish server address
	public String trigger;
	public Map<String, ServerInfo> serverMap = new HashMap<String, ServerInfo>();
}
