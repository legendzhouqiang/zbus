package io.zbus.unittests;

import io.zbus.kit.JsonKit;
import io.zbus.transport.ServerAddress;

public class Test {

	public static void main(String[] args) { 
		ServerAddress sa = new ServerAddress();
		sa.address = "localhost:15555";
		sa.certificate = "xxx";
		sa.token = "abc";
		
		String json = JsonKit.toJSONString(sa);
		System.out.println(json);
	}

}
