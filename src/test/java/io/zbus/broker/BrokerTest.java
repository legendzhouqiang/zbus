package io.zbus.broker;

import io.zbus.mq.broker.SingleBroker;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;

public class BrokerTest {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		final SingleBroker broker = new SingleBroker("127.0.0.1:15555"); 
		
		broker.onConnected(new ConnectedHandler() { 
			@Override
			public void onConnected() { 
				System.out.println(broker + "connected");
			}
		});
		
		broker.onDisconnected(new DisconnectedHandler() {
			
			@Override
			public void onDisconnected() { 
				System.out.println(broker + "disconnected");
			}
		});

		//broker.close(); 
	}

}
