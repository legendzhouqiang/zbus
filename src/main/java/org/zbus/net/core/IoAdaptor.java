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
package org.zbus.net.core;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public abstract class IoAdaptor implements Codec{ 
	
	private Codec codec;
	
	public IoAdaptor codec(Codec codec){
		this.codec = codec;
		return this;
	}
	
	@Override
	public Object decode(IoBuffer buff) {
		return codec.decode(buff); 
	}
	
	@Override
	public IoBuffer encode(Object msg) { 
		return codec.encode(msg);
	}
	
	/**
	 * 服务器端侦听到链接接入回调，此时Session尚未注册，默认注册该Session
	 * @param sess
	 * @throws IOException
	 */
	protected void onSessionAccepted(Session sess) throws IOException { 
		sess.dispatcher().registerSession(SelectionKey.OP_READ, sess); 
	}
	/**
	 * Session注册到Dispatcher成功后回调
	 * @param sess
	 * @throws IOException
	 */
	protected void onSessionRegistered(Session sess) throws IOException {  
	
	} 
	/**
	 * 客户端链接成功后回调
	 * @param sess
	 * @throws IOException
	 */
	protected void onSessionConnected(Session sess) throws IOException{
		//默认关注读写事件
		sess.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
	}
	
	/**
	 * Session注销前回调
	 * @param sess
	 * @throws IOException
	 */
	protected void onSessionToDestroy(Session sess) throws IOException{
		
	}
	
	/**
	 * Session注销后回调
	 * @param sess
	 * @throws IOException
	 */
	protected void onSessionDestroyed(Session sess) throws IOException{
		
	}
	/**
	 * Session接受到消息对象
	 * @param msg
	 * @param sess
	 * @throws IOException
	 */
	protected void onMessage(Object msg, Session sess) throws IOException{
		
	}
	/**
	 * Session各类错误发生时回调
	 * @param e
	 * @param sess
	 * @throws IOException
	 */
	protected void onException(Throwable e, Session sess) throws IOException{
		if(e instanceof IOException){
			throw (IOException) e;
		} else if (e instanceof RuntimeException){
			throw (RuntimeException)e;
		} else {
			throw new RuntimeException(e.getMessage(), e); //rethrow by default
		}
	}
}
