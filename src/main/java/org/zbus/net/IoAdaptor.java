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
package org.zbus.net;

import java.io.IOException;
/**
 * IoAdaptor is the core configurable entry that a networking application should be focus on, 
 * it personalizes the life cycle events of networking such as session accepted, connected, registered,
 * on error and etc. It is pretty simple compared to netty channel chain design.
 * 
 * @author rushmore (洪磊明)
 *
 */
public interface IoAdaptor{  
	
	/**
	 * Server side event when server socket accept an incoming client socket.
	 * Session is still unregister during this stage, so the default action is to register it,
	 * typically with OP_READ interested, application can change this behavior for special usage.
	 * 
	 * This event is for server only
	 * 
	 * @param sess Session generated after accept of server channel
	 * @throws IOException if fails
	 */
	void onSessionAccepted(Session sess) throws IOException;
	/**
	 * Triggered after session registered, omit this event by default
	 * 
	 * @param sess session registered
	 * @throws IOException if fails
	 */
	void onSessionRegistered(Session sess) throws IOException;
	/**
	 * Triggered after initiative client connection(session) is successful
	 * 
	 * This event is for client only
	 * 
	 * @param sess connected session
	 * @throws IOException if fails
	 */
	void onSessionConnected(Session sess) throws IOException;
	
	/**
	 * Triggered before session is going to be destroyed, session is still legal in this stage
	 * 
	 * @param sess session to be destroyed
	 * @throws IOException if fails
	 */
	void onSessionToDestroy(Session sess) throws IOException;
	
	/**
	 * Triggered when application level messaged is fully parsed(well framed).
	 * 
	 * @param msg application level message decided by the codec
	 * @param sess message generating session
	 * @throws IOException if fails
	 */
	void onMessage(Object msg, Session sess) throws IOException;
	/**
	 * Triggered when session error caught
	 * @param e error ongoing
	 * @param sess corresponding session
	 * @throws IOException if fails
	 */
	void onException(Throwable e, Session sess) throws Exception;
}
