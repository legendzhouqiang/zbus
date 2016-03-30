package org.zbus.examples.net.server;

import org.zbus.net.Server;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.net.http.MessageAdaptor;

public class MyServer {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		// create server with SelectorGroup
		final SelectorGroup dispatcher = new SelectorGroup();
		final Server server = new Server(dispatcher);

		//using a MessageAdaptor(HTTP protocol compatible)
		MessageAdaptor ioAdaptor = new MessageAdaptor();
		ioAdaptor.uri("/hello", new MessageProcessor() { //uri also as cmd in zbus protocol
			@Override
			public Message process(Message request) {
				Message resp = new Message();
				resp.setStatus(200);
				resp.setBody("hello" + System.currentTimeMillis());
				return resp;
			}
		});

		// start adaptor on port 8080
		server.start(8080, ioAdaptor);
	}
}
