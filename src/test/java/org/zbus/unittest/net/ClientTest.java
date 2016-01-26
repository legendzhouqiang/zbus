package org.zbus.unittest.net;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.MessageClient;

public class ClientTest { 
	SelectorGroup group;
	
	@Before
	public void setUp() throws Exception {
		group = new SelectorGroup();
	}

	@After
	public void tearDown() throws Exception {
		group.close();
	}

	@Test
	public void test() throws IOException { 
		MessageClient client = new MessageClient("127.0.0.1:15555", group);
		try{
			client.connectSync();
		}catch(IOException e){
			System.err.println(e);
		}
		client.close(); 
		
	}

}
