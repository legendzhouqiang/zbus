package io.zbus.mq.broker;

import java.io.IOException;

import io.zbus.mq.BrokerConfig;

public class TrackBroker extends MultiBroker {    
	
	public TrackBroker(BrokerConfig config) throws IOException{   
		super(config);
	}  
}

