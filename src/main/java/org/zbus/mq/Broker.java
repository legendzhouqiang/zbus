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
package org.zbus.mq;

import java.io.Closeable;
import java.io.IOException;

import org.zbus.net.ResultCallback;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;


/**
 * Broker是zbus节点抽象，可以使单节点zbus也可是多节点高可用抽象
 * 提供链接管理（连接池、创建或者销毁链接）、消息代理
 * 
 * 注意： Broker是重量级对象（带有连接池以及NIO执行线程池等），所以调用方不应该频繁创建销毁，应该等同于
 * 数据库DB连接池模式使用。
 * 
 * @author 洪磊明(rushmore)
 *
 */
public interface Broker extends Closeable{
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

	/**
	 * 请求Broker代理执行一个zbus消息请求，异步消息模式
	 * 
	 * 具体请求算法，由实现确定，单节点时采用不做节点探测失败，多节点时做节点负载均衡和容错
	 * @param msg
	 * @param callback
	 * @throws IOException
	 */
	void invokeAsync(Message msg, final ResultCallback<Message> callback) throws IOException;
	
	/**
	 * 请求Broker代理执行一个zbus消息请求，同步消息模式
	 * 
	 * 具体请求算法，由实现确定，单节点时采用不做节点探测失败，多节点时做节点负载均衡和容错
	 * @param req
	 * @param timeout
	 * @return
	 * @throws IOException
	 */
	Message invokeSync(Message req, int timeout) throws IOException;  
	
	
	public static class ClientHint { 
		private String mq;
		private String broker;  
		
		public String getMq() {
			return mq;
		}
		public void setMq(String mq) {
			this.mq = mq;
		}
		public String getBroker() {
			return broker;
		}
		public void setBroker(String broker) {
			this.broker = broker;
		} 
	}
}
