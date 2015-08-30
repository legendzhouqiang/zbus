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

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.zbus.kit.NetKit;
import org.zbus.log.Logger;

public class Session implements Closeable{
	public static enum SessionStatus {
		NEW, CONNECTED, AUTHENED, ON_ERROR, CLOSED
	}
	private static final Logger log = Logger.getLogger(Session.class); 
	
	private SessionStatus status = SessionStatus.NEW;
	private long lastOperationTime = System.currentTimeMillis();
	private final String id; 
	
	private int bufferSize = 1024*8;
	private IoBuffer readBuffer = null;
	private Queue<ByteBuffer> writeBufferQ = new LinkedBlockingQueue<ByteBuffer>();
	
	private CountDownLatch connectLatch = new CountDownLatch(1);
	
	private final Dispatcher dispatcher;
	private final SocketChannel channel;
	private SelectionKey registeredKey = null;
	
	private ConcurrentMap<String, Object> attributes = null;

	private Object attachment;
	private volatile IoAdaptor ioAdaptor;
	public Session chain; //for chain
	
	public Session(Dispatcher dispatcher, SocketChannel channel, IoAdaptor ioAdaptor){
		this(dispatcher, channel, null, ioAdaptor); 
	}
	
	public Session(Dispatcher dispatcher, SocketChannel channel, Object attachment, IoAdaptor ioAdaptor){
		this.dispatcher = dispatcher;
		this.id = UUID.randomUUID().toString();
		this.channel = channel; 
		this.attachment = attachment;
		this.ioAdaptor = ioAdaptor;
	}
	
	
	
	public String id(){
		return this.id;
	} 
	
	public Dispatcher getDispatcher(){
		return this.dispatcher;
	}
	
	public void close() throws IOException {
		if(this.status == SessionStatus.CLOSED){
			return;
		}
		this.status = SessionStatus.CLOSED;
		
		//放到channel.close前面，避免ClosedChannelException
		if(this.registeredKey != null){
			this.registeredKey.cancel();
			this.registeredKey = null;
		} 
		
		if(this.channel != null){
			this.channel.close();  
		}  
		
	}
	
	public void asyncClose() throws IOException{ 
		if(this.registeredKey == null){
			return;
		} 
		SelectorThread selector = dispatcher.getSelector(this.registeredKey);
		if(selector == null){
			throw new IOException("failed to find dispatcher for session: "+this);
		}
		
		selector.unregisterSession(this);
	}
	
	public void write(Object msg) throws IOException{
		write(ioAdaptor.encode(msg));
	}
	
	public void write(IoBuffer buf) throws IOException{
		if(this.registeredKey == null){
			throw new IOException("Session not registered yet:"+this);
		}
		
		if(!writeBufferQ.offer(buf.buf())){
			String msg = "Session write buffer queue is full, message count="+writeBufferQ.size();
			log.warn(msg);
			throw new IOException(msg);
		}
		
		registeredKey.interestOps(registeredKey.interestOps() | SelectionKey.OP_WRITE);
		registeredKey.selector().wakeup(); //TODO
	}
	
	
	
	public void doRead() throws IOException { 
		if(readBuffer == null){
			readBuffer = IoBuffer.allocate(bufferSize);
		}
		ByteBuffer data = ByteBuffer.allocate(1024*4);
		
		int n = 0;
		while((n = channel.read(data)) > 0){
			data.flip();
			readBuffer.writeBytes(data.array(), data.position(), data.remaining());
			data.clear();
		}
		
		if(n < 0){
			ioAdaptor.onSessionDestroyed(this);
			asyncClose(); 
			return;
		} 
		
		IoBuffer tempBuf = readBuffer.duplicate().flip();
		Object msg = null;
		while(true){
			tempBuf.mark();
			if(tempBuf.remaining()>0){
				msg = ioAdaptor.decode(tempBuf);
			} else {
				msg = null;
			}
			if(msg == null){ 
				tempBuf.reset();
				readBuffer = resetIoBuffer(tempBuf);
				break;
			}
			
			final Object theMsg = msg;  
			dispatcher.asyncRun(new Runnable() { 

				public void run() { 
					try{
						ioAdaptor.onMessage(theMsg, Session.this);
					} catch(Throwable e){ 
						log.error(e.getMessage(), e);
						try {
							ioAdaptor.onException(e, Session.this);
						} catch (IOException e1) { 
							try {
								close();
							} catch (Throwable e2) {
								log.error(e2.getMessage(), e2);
							}
						}  
					}
				}
			});  
			
		}  
		
	}
	protected IoBuffer resetIoBuffer(IoBuffer buffer) {
		IoBuffer newBuffer = null;

		if (buffer != null && buffer.remaining() > 0) {
			int len = buffer.remaining();
			byte[] bb = new byte[len];
			buffer.readBytes(bb);
			newBuffer = IoBuffer.wrap(bb);
			newBuffer.position(len);
		}

		return newBuffer;
	}
	
	protected int doWrite() throws IOException{ 
		int n = 0;
		synchronized (writeBufferQ) {
			while(true){
				ByteBuffer buf = writeBufferQ.peek();
				if(buf == null){
					registeredKey.interestOps(SelectionKey.OP_READ);
					//registeredKey.selector().wakeup(); //TODO
					break;
				}
				
				int wbytes = this.channel.write(buf);
				
				if(wbytes == 0 && buf.remaining() > 0){
					//registeredKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
					//registeredKey.selector().wakeup(); //TODO
					break;
				}
				
				n += wbytes;
				if(buf.remaining() == 0){
					writeBufferQ.remove();
					continue;
				} else {
					break;
				}
			} 
		}
		return n;
	}
	
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) { 
		if(obj instanceof Session){ 
			Session other = (Session)obj;
			return (this.hashCode() == other.hashCode());
		}
		return false;
	}

	public long getLastOperationTime() {
		return lastOperationTime;
	}

	public void updateLastOperationTime() {
		this.lastOperationTime = System.currentTimeMillis();
	}  
	
	public String getRemoteAddress() {
		if (this.status != SessionStatus.CLOSED) { 
			InetAddress addr = this.channel.socket().getInetAddress();
			if(addr == null) return null;
			if(channel.socket() == null) return null;
			return String.format("%s:%d", addr.getHostAddress(),channel.socket().getPort());
		} 
		return null;
	}
	
	public String getLocalAddress() {
		if (this.status != SessionStatus.CLOSED) { 
			return NetKit.localAddress(this.channel);
		} 
		return null;
	}

	public int interestOps() throws IOException{
		if(this.registeredKey == null){
			throw new IOException("Session not registered yet:"+this);
		}
		return this.registeredKey.interestOps();
	}
	
	public void register(int interestOps) throws IOException{
		dispatcher.registerSession(interestOps, this);
	}
	
	public void interestOps(int ops){
		if(this.registeredKey == null){
			throw new IllegalStateException("registered session required");
		}
		this.registeredKey.interestOps(ops); 
	}
	
	public void interestOpsAndWakeup(int ops){
		interestOps(ops);
		this.registeredKey.selector().wakeup();
	}

	public SelectionKey getRegisteredKey() {
		return registeredKey;
	}
	
	public void setRegisteredKey(SelectionKey key) {
		this.registeredKey = key;
	}
	
	public void setIoAdaptor(IoAdaptor ioAdaptor){
		this.ioAdaptor = ioAdaptor;
	}
	
	public SessionStatus getStatus() {
		return status;
	}
	
	public boolean isActive(){
		return this.status == SessionStatus.CONNECTED;
	}
	
	public boolean isNew(){
		return this.status == SessionStatus.NEW;
	}

	public void setStatus(SessionStatus status) {
		this.status = status;
	}

	public SocketChannel getChannel() {
		return channel;
	} 
	
	public Dispatcher dispatcher() {
		return dispatcher;
	}

	public void finishConnect(){
		this.connectLatch.countDown();
	}
	
	
	public boolean waitToConnect(long millis){
		try { 
			return this.connectLatch.await(millis, TimeUnit.MILLISECONDS); 
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}
	
	public void removeAttr(String key){
		if(this.attributes == null) return;
		this.attributes.remove(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T attr(String key){
		if(this.attributes == null){
			return null;
		}
		
		return (T)this.attributes.get(key);
	}
	
	public <T> void attr(String key, T value){
		if(this.attributes == null){
			synchronized (this) {
				if(this.attributes == null){
					this.attributes = new ConcurrentHashMap<String, Object>();
				}
			} 
		}
		this.attributes.put(key, value);
	}

	public String toString() {
		return "Session ["
				+ "remote=" + getRemoteAddress()
				+ ", status=" + status  
	            + ", id=" + id   
				+ "]";
	}



	public Object getAttachment() {
		return attachment;
	}

	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	public IoAdaptor getIoAdaptor() {
		return ioAdaptor;
	}
	
}
