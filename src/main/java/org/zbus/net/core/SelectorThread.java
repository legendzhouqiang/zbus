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
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.zbus.kit.NetKit;
import org.zbus.log.Logger;
import org.zbus.net.core.Session.SessionStatus;

public class SelectorThread extends Thread {
	private static final Logger log = Logger.getLogger(SelectorThread.class);
	
	protected volatile java.nio.channels.Selector selector = null;
	protected final Dispatcher dispatcher;
	private final Queue<Object[]> register = new LinkedBlockingQueue<Object[]>();
	private final Queue<Session> unregister = new LinkedBlockingQueue<Session>(); 
	
	public SelectorThread(Dispatcher dispatcher, String name) throws IOException{
		super(name);
		this.dispatcher = dispatcher;
		this.selector = java.nio.channels.Selector.open();
	}
	
	public SelectorThread(Dispatcher dispatcher) throws IOException{
		this(dispatcher, "Selector");
	}
	
	
	public void registerChannel(SelectableChannel channel, int ops) throws IOException{
		registerChannel(channel, ops, null); 
	}
	
	public void registerSession(int ops, Session sess) throws IOException{
		registerChannel(sess.getChannel(), ops, sess);
	}
	
	public void registerChannel(SelectableChannel channel, int ops, Session sess) throws IOException{
		if(Thread.currentThread() == this){
			SelectionKey key = channel.register(this.selector, ops, sess);
			if(sess != null){
				sess.setRegisteredKey(key);
				sess.setStatus(SessionStatus.CONNECTED);
				sess.getIoAdaptor().onSessionRegistered(sess);
			} 
		} else { 
			this.register.offer(new Object[]{channel, ops, sess});
			this.selector.wakeup();
		}
	}
	
	public void unregisterSession(Session sess){
		if(this.unregister.contains(sess)){
			return;
		}
		this.unregister.add(sess);
		this.selector.wakeup();
	}
	
	
	@Override
	public void interrupt() {  
		super.interrupt();
		try {
			this.selector.close();
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
		}
	}
	
	
	@Override
	public void run() { 
		try{
			while(!isInterrupted()){
				selector.select(); 
				handleRegister(); 
				
				Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
				while(iter.hasNext()){
					SelectionKey key = iter.next();
					iter.remove();
					if(!key.isValid()) continue;
					
					Object att = key.attachment();
					if(att != null && att instanceof Session){
						((Session)att).updateLastOperationTime();
					}
					try{ 
						if(key.isAcceptable()){ 
							handleAcceptEvent(key);
						} else if (key.isConnectable()){
							handleConnectEvent(key);
						} else if (key.isReadable()){
							handleReadEvent(key);
						} else if (key.isWritable()){
							handleWriteEvent(key);
						}
					} catch(Throwable e){ 
						disconnectWithException(key, e); 
					}
				} 
				handleUnregister();
			}
		} catch(Throwable e) {
			if(!dispatcher.isStarted()){
				if(log.isDebugEnabled()){
					log.debug(e.getMessage(), e);
				}
			} else {
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private void disconnectWithException(final SelectionKey key, final Throwable e){  
		Session sess = (Session)key.attachment();
		if(sess == null){
			try{ 
				key.channel().close();   
				key.cancel();
			} catch(Throwable ex){
				log.error(e.getMessage(), ex);
			} 
			return;
		}
		
		try{ 
			sess.setStatus(SessionStatus.ON_ERROR);
			sess.getIoAdaptor().onException(e, sess);
		} catch (Throwable ex){
			if(!dispatcher.isStarted()){
				log.debug(e.getMessage(), ex);
			} else {
				log.error(e.getMessage(), ex);
			}
		}
		
		try{ 
			sess.close(); 
			key.cancel();
		} catch(Throwable ex){
			log.error(e.getMessage(), ex);
		}
	}
	
	protected void handleRegister(){
		Object[] item = null;
		while( (item=this.register.poll()) != null){
			try{
				SelectableChannel channel = (SelectableChannel) item[0];
				if (!channel.isOpen() ) continue;
				int ops = (Integer)item[1];
				Session sess = (Session) item[2]; 
				
				SelectionKey key = channel.register(this.selector, ops, sess);
				if(sess != null){
					sess.setRegisteredKey(key);
					sess.getIoAdaptor().onSessionRegistered(sess);
				} 
				
			}catch(Exception e){
				log.error(e.getMessage(), e);
			}
		}
	}
	
	protected void handleUnregister(){
		Session sess = null;
		while( (sess = this.unregister.poll()) != null ){
			try {
				sess.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}
	
	
	protected void handleAcceptEvent(SelectionKey key) throws IOException{ 
		ServerSocketChannel server = (ServerSocketChannel) key.channel();
		
		SocketChannel channel = server.accept();
		channel.configureBlocking(false); 
		
		if(log.isDebugEnabled()){ 
			log.debug("ACCEPT: server(%s) %s=>%s", NetKit.remoteAddress(channel), NetKit.localAddress(channel));
		}
		
		SocketAddress serverAddress = server.socket().getLocalSocketAddress();
		
		IoAdaptor ioAdaptor = dispatcher.ioAdaptor(serverAddress);
		if(ioAdaptor == null){
			log.warn("Missing IoAdaptor for %s", serverAddress);
			return;
		}
		
		Session sess = new Session(dispatcher, channel, ioAdaptor); 
		sess.setStatus(SessionStatus.CONNECTED); //set connected 
		
		sess.getIoAdaptor().onSessionAccepted(sess);
		
	} 
	
	protected void handleConnectEvent(SelectionKey key) throws IOException{
		final SocketChannel channel = (SocketChannel) key.channel();
		
		Session sess = (Session) key.attachment();
		if(sess == null){
			throw new IOException("Session not attached yet to SelectionKey");
		}  
		
		if(channel.finishConnect()){
			sess.finishConnect(); 
			if(log.isDebugEnabled()){
				log.debug("CONNECT: %s=>%s", NetKit.localAddress(channel), NetKit.remoteAddress(channel));
			}
		}
		sess.setStatus(SessionStatus.CONNECTED);  
		key.interestOps(0); //!!!clear interest of OP_CONNECT to avoid looping CPU !!!
		sess.getIoAdaptor().onSessionConnected(sess);
	
	}
	
	protected void handleReadEvent(SelectionKey key) throws IOException{
		Session sess = (Session) key.attachment();
		if(sess == null){
			throw new IOException("Session not attached yet to SelectionKey");
		}
		sess.doRead();
	}
	
	protected void handleWriteEvent(SelectionKey key) throws IOException{
		Session sess = (Session) key.attachment();
		if(sess == null){
			throw new IOException("Session not attached yet to SelectionKey");
		} 
		sess.doWrite();
	}

}
