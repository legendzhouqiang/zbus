package org.zbus.mq;

import org.zbus.net.core.Dispatcher;
import org.zbus.pool.PoolConfig;



public class BrokerConfig extends PoolConfig{
	private String brokerAddress = "127.0.0.1:15555";
	/**
	 * 可选项
	 * 如果配置不给出，Dispatcher内部生成，并自己管理关闭
	 * 如果配置给出，内部仅仅共享使用，不关闭
	 */
	private Dispatcher dispatcher;
	
	private int selectorCount = 1;
	private int executorCount = 16;
	public String getBrokerAddress() {
		return brokerAddress;
	}
	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}
	public Dispatcher getDispatcher() {
		return dispatcher;
	}
	public void setDispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	public int getSelectorCount() {
		return selectorCount;
	}
	public void setSelectorCount(int selectorCount) {
		this.selectorCount = selectorCount;
	}
	public int getExecutorCount() {
		return executorCount;
	}
	public void setExecutorCount(int executorCount) {
		this.executorCount = executorCount;
	}

}
