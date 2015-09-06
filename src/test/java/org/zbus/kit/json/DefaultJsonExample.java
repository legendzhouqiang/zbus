package org.zbus.kit.json;
 
import java.util.ArrayList;
import java.util.List;

import org.zbus.broker.ha.ServerEntry;
import org.zbus.kit.json.impl.DefaultJson;

public class DefaultJsonExample {

	public static void main(String[] args) { 
		DefaultJson json = new DefaultJson();
		List<ServerEntry> list = new ArrayList<ServerEntry>();
		ServerEntry se = new ServerEntry();
		se.serverAddr = "127.0.0.1:15555";
		list.add(se);
		
		String jsonString = json.toJSONString(list);
		System.out.println(list);
		System.out.println(jsonString);
		
		System.out.println(json.parseArray(jsonString, ServerEntry.class));
		
	} 
}
