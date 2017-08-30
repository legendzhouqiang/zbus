package org.zbus.examples.net.exceptions;

import java.io.IOException;

import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.MessageClient;

public class ConnectFailure {

	public static void main(String[] args) throws Exception { 
		SelectorGroup group = new SelectorGroup();
		MessageClient client = new MessageClient("127.0.0.1:15555", group);
		
		try {
			client.connectSync();
		} catch (IOException e) { 
			System.err.println(">>>>"+e.getMessage());
			e.printStackTrace();
		} finally {
			client.close();
			group.close();
		}

	}

}
