package org.zbus.examples.net.mysql;

import java.io.IOException;

import org.zbus.examples.net.mysql.MysqlMessage.MysqlMessageHandler;
import org.zbus.net.Server;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.core.Session;

public class MySqlServer {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		final SelectorGroup dispatcher = new SelectorGroup();
		final Server server = new Server(dispatcher);

		MysqlMessageAdaptor ioAdaptor = new MysqlMessageAdaptor();
		
		ioAdaptor.cmd("1", new MysqlMessageHandler() {
			
			@Override
			public void handle(MysqlMessage request, Session sess) throws IOException {
				System.out.println(" "+request.pakgId +" "+request.length);
			}
		});
		

		server.start(5050, ioAdaptor);
	}
}
