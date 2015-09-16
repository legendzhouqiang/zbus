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
package org.zbus.proxy;
 

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicLong;

import org.zbus.kit.log.Logger;
import org.zbus.net.core.Codec;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.IoBuffer;
import org.zbus.net.core.Session;

class ProxyCodec implements Codec{
	
	@Override
	public IoBuffer encode(Object msg) { 
		if(msg instanceof IoBuffer){
			IoBuffer buff = (IoBuffer)msg;
			return buff;
		} else { 
			throw new RuntimeException("Message Not Support"); 
		} 
	}
       
	@Override
	public Object decode(IoBuffer buff) { 
		if(buff.remaining()>0){ 
			byte[] data = new byte[buff.remaining()];
			buff.readBytes(data);
			return IoBuffer.wrap(data); 
		} else {
			return null;
		} 
	}
}
public class BindingAdaptor extends IoAdaptor{ 
	private static final Logger log = Logger.getLogger(BindingAdaptor.class); 
	protected final AtomicLong bindedCount = new AtomicLong(0);
	public BindingAdaptor(){
		codec(new ProxyCodec());
	}
	
	@Override
	protected void onMessage(Object msg, Session sess) throws IOException {  
		Session chain = sess.chain;
		if(chain == null){
			sess.asyncClose(); 
			return;
		} 
		chain.write(msg); 
	}
	 
	@Override
	public void onSessionConnected(Session sess) throws IOException {  
		Session chain = sess.chain;
		if(chain == null){
			log.info("Missing chain");
			sess.asyncClose();
			return; 
		}   
		if(sess.isActive() && chain.isActive()){
			log.info("~~~bind(%d)~~~\n%s\n%s", bindedCount.incrementAndGet(), sess.chain, sess);
			sess.register(SelectionKey.OP_READ);
			chain.register(SelectionKey.OP_READ);
		}
	}
	
	@Override
	public void onSessionToDestroy(Session sess) throws IOException {  
		cleanSession(sess);
	}
	
	public void cleanSession(final Session sess) {
		try {
			sess.close();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		
		if (sess.chain == null) return;
		log.info("<<<detach(%d)>>>\n%s\n%s", bindedCount.decrementAndGet(), sess, sess.chain); 
		try {	
			sess.chain.close();	
			sess.chain.chain = null;
			sess.chain = null;
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
}
