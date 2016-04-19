package org.zbus.examples.net;

import java.io.File;

import org.zbus.net.http.Message;

public class MessageTest {

	public static void main(String[] args) throws Exception {
		Message msg = new Message();
		msg.setBody(new File("pom.xml")); 
		System.out.println(msg);
	}

}
