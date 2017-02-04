package io.zbus.broker;

import io.zbus.mq.broker.SingleBroker;
import io.zbus.mq.broker.SingleBroker.BrokerConnectedHandler;
import io.zbus.mq.broker.SingleBroker.BrokerDisconnectedHandler;

public class BrokerTest {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		SingleBroker broker = new SingleBroker("127.0.0.1:15555"); 
		
		broker.onBrokerConnected(new BrokerConnectedHandler() { 
			@Override
			public void onConnected(SingleBroker broker) { 
				System.out.println(broker + "connected");
			}
		});
		
		broker.onBrokerDisconnected(new BrokerDisconnectedHandler() {
			
			@Override
			public void onDisconnected(SingleBroker broker) { 
				System.out.println(broker + "disconnected");
			}
		});

		//broker.close(); 
	}

}
