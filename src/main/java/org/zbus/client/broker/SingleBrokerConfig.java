package org.zbus.client.broker;

import org.zbus.remoting.pool.RemotingClientPoolConfig;


public class SingleBrokerConfig extends RemotingClientPoolConfig{
	@Override
	public SingleBrokerConfig clone() { 
		return (SingleBrokerConfig)super.clone();
	}
}
