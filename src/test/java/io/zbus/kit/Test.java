package io.zbus.kit;

import io.zbus.mq.model.Channel;

public class Test {
	public static void main(String[] args) {
		Channel c = new Channel();
		c.name = "xxx";
		c.offset = 123; 
		
		Channel c2 = c.clone();
		
		c2.offset = 321;
		c2.name = "xxx2"; 
		
		System.out.println(JsonKit.toJSONString(c));
		System.out.println(JsonKit.toJSONString(c2));
	}
}
