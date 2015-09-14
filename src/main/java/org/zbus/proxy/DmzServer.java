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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.zbus.kit.ConfigKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.Server;
import org.zbus.net.core.Codec;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.IoBuffer;
import org.zbus.net.core.Session;


class NotifyCodec implements Codec{ 
	@Override
	public IoBuffer encode(Object msg) { 
		if(msg instanceof Integer){
			Integer c = (Integer)msg;
			IoBuffer buff = IoBuffer.allocate(4);
			buff.writeInt(c);
			return buff.flip();
		} else { 
			throw new RuntimeException("Message Not Support"); 
		} 
	}
       
	@Override
	public Object decode(IoBuffer buff) { 
		if(buff.remaining()>=4){ 
			return buff.readInt();
		} else {
			return null;
		} 
	}
}

class NotifyAdaptor extends IoAdaptor{ 
	private static final Logger log = Logger.getLogger(NotifyAdaptor.class);  
	private List<Session> sessions = Collections.synchronizedList(new ArrayList<Session>());
	public AtomicInteger count = new AtomicInteger(0);
	public AtomicInteger index = new AtomicInteger(0);
	
	public NotifyAdaptor() { 
		codec(new NotifyCodec());
	}
	
	@Override
	protected void onSessionAccepted(Session sess) throws IOException { 
		super.onSessionAccepted(sess);
		sessions.add(sess);
	}
	
	@Override
	protected void onException(Throwable e, Session sess) throws IOException {
		sessions.remove(sess);
		super.onException(e, sess);
	}
	
	@Override
	protected void onSessionToDestroy(Session sess) throws IOException {
		sessions.remove(sess);
		super.onSessionToDestroy(sess);
	}  
	
	public void notifyDownstream(){
		synchronized(count){
			count.incrementAndGet();
			if(sessions.size() > 0){
				int idx = index.get()%sessions.size();
				Session sess = sessions.get(idx);
				try {
					if(log.isDebugEnabled()){
						log.debug("Request connections: " + count.get());
					}
					sess.write(count.get());
					count.set(0);
				} catch (IOException e) {
					e.printStackTrace();
					sessions.remove(sess);
				}
			}
		}
	}
}


class ServerBindingAdaptor extends BindingAdaptor{  
	public final BlockingQueue<Session> myPendings = new ArrayBlockingQueue<Session>(1024);
	public ServerBindingAdaptor peer; 
	public NotifyAdaptor notify;
	
	public ServerBindingAdaptor(){
		codec(new ProxyCodec());
	}
	
	@Override
	protected void onSessionAccepted(Session sess) throws IOException { 
		Session peerSess = peer.myPendings.poll();  
		if(peerSess == null){
			myPendings.offer(sess);
			if(notify != null){ //上游具有通知机制，通知target链接上来
				notify.notifyDownstream();
			}
			return;
		} 
		
		sess.chain = peerSess;
		peerSess.chain = sess; 
		
		sess.register(SelectionKey.OP_READ);
		peerSess.register(SelectionKey.OP_READ);
	}
}

public class DmzServer{  
	private static final Logger log = Logger.getLogger(DmzServer.class);  
	public static void main(String[] args) throws Exception {  
		int up = ConfigKit.option(args, "-up", 8080);
		int down = ConfigKit.option(args, "-down", 15557);
		int notify = ConfigKit.option(args, "-notify", 15558);
		
		NotifyAdaptor notifyAdaptor = new NotifyAdaptor();
        ServerBindingAdaptor upAdaptor = new ServerBindingAdaptor();
        ServerBindingAdaptor downAdaptor = new ServerBindingAdaptor();
        
        upAdaptor.peer = downAdaptor;
        downAdaptor.peer = upAdaptor;
        upAdaptor.notify = notifyAdaptor;
        
        final Dispatcher dispatcher = new Dispatcher();
        final Server dmzServer = new Server(dispatcher);
        dmzServer.registerAdaptor(up, upAdaptor, "Up");
        dmzServer.registerAdaptor(down, downAdaptor, "Down");
        dmzServer.registerAdaptor(notify, notifyAdaptor, "Notify");
        dmzServer.setServerName("DMZServer");
        
        dmzServer.start(); 
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try {
					dmzServer.close();
					dispatcher.close();
					log.info("DmzServer shutdown completed");
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}); 
	}
}
