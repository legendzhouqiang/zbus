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
package org.zbus.rpc.direct;

import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.Message.MessageProcessor;

public class ServiceConfig {  
	public String serverHost = "0.0.0.0";
	public int serverPort = 0;  
	public int selectorCount = 0;
	public int executorCount = 0; 
	public Dispatcher dispatcher;
	public MessageProcessor messageProcessor;
	
	//如果加入高可用HA才需要填写
	public String trackServerList; 
	public String entryId;
	public String getServerHost() {
		return serverHost;
	}
	
	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}
	public int getServerPort() {
		return serverPort;
	}
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
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
	public Dispatcher getDispatcher() {
		return dispatcher;
	}
	public void setDispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	public MessageProcessor getMessageProcessor() {
		return messageProcessor;
	}
	public void setMessageProcessor(MessageProcessor messageProcessor) {
		this.messageProcessor = messageProcessor;
	}
	
	public String getTrackServerList() {
		return trackServerList;
	}
	public void setTrackServerList(String trackServerList) {
		this.trackServerList = trackServerList;
	}
	public String getEntryId() {
		return entryId;
	}
	public void setEntryId(String entryId) {
		this.entryId = entryId;
	} 
}
