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

import java.io.Closeable;
import java.io.IOException;

import org.zbus.kit.NetKit;
import org.zbus.net.http.Message.MessageInvoker;


public interface Broker extends MessageInvoker, Closeable{ 
	MessageInvoker getClient(BrokerHint hint) throws IOException; 
	void closeClient(MessageInvoker client) throws IOException; 
	
	public static class BrokerHint {   
		private static final String requestIp = NetKit.getLocalIp();
		private String server;  
		private String entry;   
		
		public String getRequestIp(){
			return requestIp;
		} 
		public String getEntry() {
			return entry;
		}
		public void setEntry(String entry) {
			this.entry = entry;
		}
		public String getServer() {
			return server;
		}
		public void setServer(String server) {
			this.server = server;
		}  
	}
}
