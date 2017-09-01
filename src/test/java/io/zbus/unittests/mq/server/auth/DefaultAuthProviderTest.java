package io.zbus.unittests.mq.server.auth;

import io.zbus.mq.server.auth.DefaultAuthProvider;

public class DefaultAuthProviderTest {

	public static void main(String[] args) { 
		DefaultAuthProvider auth = new DefaultAuthProvider();
		auth.loadFromXml("conf/zbus1.xml"); 
	}

}
