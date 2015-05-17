package org.zstacks.zbus.client.broker;

import org.zstacks.znet.pool.RemotingClientPoolConfig;


public class SingleBrokerConfig extends RemotingClientPoolConfig{
	@Override
	public SingleBrokerConfig clone() { 
		return (SingleBrokerConfig)super.clone();
	}
}
