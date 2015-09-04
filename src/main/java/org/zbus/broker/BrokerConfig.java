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

import org.zbus.kit.pool.PoolConfig;
import org.zbus.net.core.Dispatcher;



public class BrokerConfig extends PoolConfig{
	private String trackServerList = "127.0.0.1:16666"; //只在HA模式下才有效
	private String serverAddress   = "127.0.0.1:15555"; 
	private int selectorCount = 1;
	private int executorCount = 64; 
	/**
	 * 可选项
	 * 如果配置不给出，Dispatcher内部生成，并自己管理关闭
	 * 如果配置给出，内部仅仅共享使用，不关闭
	 */
	private Dispatcher dispatcher;
	
	public String getServerAddress() {
		return serverAddress;
	}
	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
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
	
	public String getTrackServerList() {
		return trackServerList;
	}
	public void setTrackServerList(String trackServerList) {
		this.trackServerList = trackServerList;
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
