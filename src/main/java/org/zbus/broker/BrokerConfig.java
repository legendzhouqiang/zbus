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
package org.zbus.broker;

/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2016 HONG LEIMING
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
import org.zbus.kit.pool.PoolConfig;
import org.zbus.mq.server.MqServer;
import org.zbus.mq.server.MqServerConfig;
import org.zbus.net.core.SelectorGroup;

public class BrokerConfig extends PoolConfig{ 
	/**
	 * broker address abstraction: 
	 * 1) single broker use zbus address
	 * 2) ha broker, use ha trackserver list, ; split
	 * 3) jvm broker 
	 */
	private String brokerAddress = "127.0.0.1:15555"; //set to null or jvm if use JvmBroker
	
	private int selectorCount = 0; //0代表使用默认值
	private int executorCount = 0; //0代表使用默认值 
	private SelectorGroup selectorGroup; //optional 
	
	//the following two items are designed to create JvmBroker
	//priority: mqServer > mqServerConfig
	private MqServer mqServer; //optional, used only for JvmBroker if supplied
	private MqServerConfig mqServerConfig; // config to create MqServer, optional if mqServer not supplied
	
	public BrokerConfig(){
		
	}
	
	public BrokerConfig(String brokerAddress){
		this.brokerAddress = brokerAddress;
	}
	
	public String getBrokerAddress() {
		return brokerAddress;
	}
	public void setBrokerAddress(String brokerAddress) {
		this.brokerAddress = brokerAddress;
	}

	public SelectorGroup getSelectorGroup() {
		return selectorGroup;
	}
	public void setSelectorGroup(SelectorGroup selectorGroup) {
		this.selectorGroup = selectorGroup;
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
	public MqServer getMqServer() {
		return mqServer;
	}
	public void setMqServer(MqServer mqServer) {
		this.mqServer = mqServer;
	} 
	
	public MqServerConfig getMqServerConfig() {
		return mqServerConfig;
	}
	public void setMqServerConfig(MqServerConfig mqServerConfig) {
		this.mqServerConfig = mqServerConfig;
	}
	@Override
	public BrokerConfig clone() { 
		try {
			return (BrokerConfig)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
}
