package io.zbus.mq;

import java.io.IOException;

import io.zbus.net.ServerAdaptor;
import io.zbus.net.Session;

public class MqttServerAdaptor extends ServerAdaptor {

	@Override
	public void onMessage(Object msg, Session sess) throws IOException { 
		
	}

}
