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
package io.zbus.net;

import java.io.IOException;
 
 

/**
 * Session life-cycle events management. This adaptor is very important point to configure user specific behavior.
 * 
 * Default implementation from NettyToIoAdaptor(Netty-Impl)
 * 
 * @author rushmore (洪磊明)
 *
 */
public interface IoAdaptor{  
	/**
	 * Triggered when session created(Netty-Impl as channelActive)
	 * @param sess Session data
	 * @throws IOException
	 */
	void sessionCreated(Session sess) throws IOException; 
	
	/**
	 * Triggered when session destroyed(Netty-Impl as channelDeactive)
	 * @param sess
	 * @throws IOException
	 */
	void sessionToDestroy(Session sess) throws IOException; 
	
	/**
	 * Triggered when session get message ready(Netty-Impl as channelRead)
	 * @param msg
	 * @param sess
	 * @throws IOException
	 */
	void sessionMessage(Object msg, Session sess) throws IOException; 
	
	/**
	 * Triggered when session encounter error ready(Netty-Impl as exceptionCaught)
	 * @param e
	 * @param sess
	 * @throws Exception
	 */
	void sessionError(Throwable e, Session sess) throws Exception;
	
	/**
	 * Triggered when session idled for a defined time period(Netty-Impl as userEventTriggered)
	 * 
	 * @param sess
	 * @throws IOException
	 */
	void sessionIdle(Session sess) throws IOException; 
}
