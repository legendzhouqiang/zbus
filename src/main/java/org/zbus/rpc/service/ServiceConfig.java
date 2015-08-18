package org.zbus.rpc.service;

import org.zbus.mq.Broker;
import org.zbus.mq.MqConfig;
import org.zbus.mq.Protocol.MqMode;

public class ServiceConfig extends MqConfig {
	
	private ServiceHandler serviceHandler;
	private int threadCount = 20;
	private int consumerCount = 1; 
	private Broker[] brokers;

	public ServiceConfig() { 
		mode = MqMode.intValue(MqMode.MQ, MqMode.Memory);
	}

	public ServiceConfig(Broker... brokers) {
		this.brokers = brokers;
		if (brokers.length > 0) {
			setBroker(brokers[0]);
		}
		mode = MqMode.intValue(MqMode.MQ, MqMode.Memory);
	}

	public void setBrokers(Broker[] brokers) {
		this.brokers = brokers;
		if (brokers.length > 0) {
			setBroker(brokers[0]);
		}
		
	}

	public Broker[] getBrokers() {
		if (brokers == null || brokers.length == 0) {
			if (getBroker() != null) {
				brokers = new Broker[] { getBroker() };
			}
		}
		return this.brokers;
	}

	public int getThreadCount() {
		return threadCount;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	public int getConsumerCount() {
		return consumerCount;
	}

	public void setConsumerCount(int consumerCount) {
		this.consumerCount = consumerCount;
	}

	public ServiceHandler getServiceHandler() {
		return serviceHandler;
	}

	public void setServiceHandler(ServiceHandler serviceHandler) {
		this.serviceHandler = serviceHandler;
	}
	
	@Override
	public ServiceConfig clone() {
		return (ServiceConfig) super.clone();
	}

}
