/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
