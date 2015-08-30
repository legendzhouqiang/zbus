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

import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.net.http.MessageClient;


public interface Broker extends MessageInvoker, Closeable{
	/**
	 * 向Broker索取一个链接对象
	 * @param hint
	 * @return
	 * @throws IOException
	 */
	MessageClient getClient(ClientHint hint) throws IOException;
	/**
	 * 通知Broker可以关闭当前链接（具体是否关闭视实现而定，带有连接池功能，一般不执行物理关闭）
	 * @param client
	 * @throws IOException
	 */
	void closeClient(MessageClient client) throws IOException;
	
	public static class ClientHint { 
		private String entry;
		private String server;  
		
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
