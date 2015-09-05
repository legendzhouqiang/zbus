package org.zbus.kit.json;

import org.zbus.kit.json.impl.DefaultJson;
import org.zbus.mq.Protocol.MqMode;

public class DefaultJonsExample {

	public static void main(String[] args) { 
		DefaultJson json = new DefaultJson();
 		Object obj = json.toJson(MqMode.MQ);
 		System.out.println(obj);
 		
		MqMode mode = json.parseObject(obj.toString(), MqMode.class);
		System.out.println(mode);
	} 
}
