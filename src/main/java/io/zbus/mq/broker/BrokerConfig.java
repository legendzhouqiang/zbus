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
package io.zbus.mq.broker;

import io.zbus.mq.broker.HaBroker.BrokerSelector;
import io.zbus.net.EventDriver;

/**
 * Only brokerAddress is required, other configurations are all optional broker
 * address abstraction: 
 * 1) single broker use zbus address 
 * 2) ha broker, use ha trackserver list, use ';' to split, '[]' required if only one address.
 * 	eg [127.0.0.1:15555], [127.0.0.1:15555;127.0.0.1:25555], 127.0.0.1:15555;127.0.0.1:25555 
 * 3) jvm broker, set to null or jvm if use JvmBroker
 * 
 * @author rushmore (洪磊明)
 *
 */
public class BrokerConfig {
	/**
	 * broker address abstraction: 
	 * 1) single broker use zbus address 
	 * 2) ha broker, use ha trackserver list, ; split  
	 */
	public String brokerAddress = "127.0.0.1:15555";  
	/** EventDriver support */
	public EventDriver eventDriver; 
	public int connectionPoolMaxSize = 32; 

	// HaBroker Configuration
	public BrokerSelector brokerSelector; 
	
	public BrokerConfig() {
	}

	public BrokerConfig(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}

	public String getBrokerAddress() {
		return brokerAddress;
	}

	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}

	public EventDriver getEventDriver() {
		return eventDriver;
	}

	public void setEventDriver(EventDriver eventDriver) {
		this.eventDriver = eventDriver;
	} 
	public BrokerSelector getBrokerSelector() {
		return brokerSelector;
	}

	public void setBrokerSelector(BrokerSelector brokerSelector) {
		this.brokerSelector = brokerSelector;
	} 

	public int getConnectionPoolMaxSize() {
		return connectionPoolMaxSize;
	}

	public void setConnectionPoolMaxSize(int connectionPoolMaxSize) {
		this.connectionPoolMaxSize = connectionPoolMaxSize;
	}

	@Override
	public BrokerConfig clone() {
		try {
			return (BrokerConfig) super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
