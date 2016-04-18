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
package org.zbus.net.simple;

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
import org.zbus.kit.log.Logger;
import org.zbus.net.IoAdaptor;
import org.zbus.net.Session;

public class DefaultSession implements Session, Closeable{
	public static enum SessionStatus {
		NEW, CONNECTED, AUTHENED, ON_ERROR, CLOSED
	}
	private static final Logger log = Logger.getLogger(DefaultSession.class); 
	
	private SessionStatus status = SessionStatus.NEW;
	private long lastOperationTime = System.currentTimeMillis();
	private final String id; 
	
	private int bufferSize = 1024*8;
	private IoBuffer readBuffer = null;
	private Queue<ByteBuffer> writeBufferQ = new LinkedBlockingQueue<ByteBuffer>();
	
	private CountDownLatch connectLatch = new CountDownLatch(1); 
	
	private final SelectorGroup selectorGroup;
	private SelectorThread selectorThread;
	private final SocketChannel channel;
	private SelectionKey registeredKey = null; 
	
	private ConcurrentMap<String, Object> attributes = null;

	private Object attachment;
	private volatile IoAdaptor ioAdaptor;
	private final Codec codec;
	public DefaultSession chain; //for chain
	
	public DefaultSession(SelectorGroup selectorGroup, 
			SocketChannel channel, IoAdaptor ioAdaptor,
			Codec codec){
		this.selectorGroup = selectorGroup;
		this.id = UUID.randomUUID().toString();
		this.channel = channel;  
		this.ioAdaptor = ioAdaptor;
		this.codec = codec;
	} 
	
	public String id(){
		return this.id;
	} 
	
	public SelectorGroup getDispatcher(){
		return this.selectorGroup;
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
		SelectorThread selector = selectorGroup.getSelector(this.registeredKey);
		if(selector == null){
			throw new IOException("failed to find dispatcher for session: "+this);
		}
		ioAdaptor.onSessionToDestroy(this);
		selector.unregisterSession(this);
	}
	
	public void write(Object msg){
		try {
			write(codec.encode(msg));
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
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
		
		if(this.selectorThread == null){
			String msg = "Session selectorThread not set";
			log.warn(msg);
			throw new IOException(msg);
		} 
		int ops = registeredKey.interestOps() | SelectionKey.OP_WRITE;
		selectorThread.interestOps(registeredKey, ops); 
	}
	
	
	
	public void doRead() throws Exception { 
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
			asyncClose(); 
			return;
		} 
		
		IoBuffer tempBuf = readBuffer.duplicate().flip();
		Object msg = null;
		while(true){
			tempBuf.mark();
			if(tempBuf.remaining()>0){
				msg = codec.decode(tempBuf);
			} else {
				msg = null;
			}
			if(msg == null){ 
				tempBuf.reset();
				readBuffer = resetIoBuffer(tempBuf);
				break;
			}
			
			final Object theMsg = msg;  
			selectorGroup.asyncRun(new Runnable() { 

				public void run() { 
					try{
						ioAdaptor.onMessage(theMsg, DefaultSession.this);
					} catch(Throwable e){ 
						log.error(e.getMessage(), e);
						try {
							ioAdaptor.onException(e, DefaultSession.this);
						} catch (Exception e1) { 
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
					selectorThread.interestOps(registeredKey, SelectionKey.OP_READ);
					//TODO wakeup?
					break;
				}
				
				int wbytes = this.channel.write(buf);
				
				if(wbytes == 0 && buf.remaining() > 0){//TODO
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
		if(obj instanceof DefaultSession){ 
			DefaultSession other = (DefaultSession)obj;
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
		InetAddress addr = this.channel.socket().getInetAddress();
		if(addr == null) return null;
		if(channel.socket() == null) return null;
		return String.format("%s:%d", addr.getHostAddress(),channel.socket().getPort()); 
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
	
	public void register(int interestOps) throws Exception{
		selectorGroup.registerSession(interestOps, this);
	}
	
	public void setSelectorThread(SelectorThread selectorThread) {
		this.selectorThread = selectorThread;
	}
	
	public void interestOps(int ops){
		if(this.registeredKey == null){
			throw new IllegalStateException("registered session required");
		}
		selectorThread.interestOps(registeredKey, ops); 
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
	
	public SelectorGroup selectorGroup() {
		return selectorGroup;
	}

	public void finishConnect(){
		this.connectLatch.countDown();
	}
	
	
	public boolean waitToConnect(long millis) throws IOException{
		try { 
			boolean status = this.connectLatch.await(millis, TimeUnit.MILLISECONDS); 
			return status;
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
		if(value == null){
			if(this.attributes != null){
				this.attributes.remove(key);
			}
			return;
		}
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
		return "DefaultSession ["
				+ "remote=" + getRemoteAddress()
				+ ", active=" + isActive()   
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

	@Override
	public void writeAndFlush(Object msg) { 
		write(msg); 
	}

	@Override
	public void flush() {
		//ignore
	} 
}
