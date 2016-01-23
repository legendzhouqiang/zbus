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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.zbus.kit.log.Logger;
 
/**
 * SelectorGroup manages Selector threads <code>SelectorThread</code>, balancing distribute the new
 * connection <code>Session</code> from clients.
 * 
 * @author rushmore (洪磊明)
 *
 */
public class SelectorGroup implements Closeable {
	public static final int DEFAULT_EXECUTOR_COUNT = 64;
	
	private static final Logger log = Logger.getLogger(SelectorGroup.class); 
	
	/* Shared thread pool for asynchronous work of the underlying network */
	private ExecutorService executor; 
	private int selectorCount = defaultSelectorCount(); //default to #CPU/2
	private int executorCount = DEFAULT_EXECUTOR_COUNT; //default to 64
	private SelectorThread[] selectors;
	private AtomicInteger selectorIndex = new AtomicInteger(0);
	private String name = "SelectorGroup";
	private String selectorNamePrefix = "Selector";
	
	
	protected volatile boolean started = false;  
	
	private Map<String, IoAdaptor> acceptIoAdaptors = new ConcurrentHashMap<String, IoAdaptor>();
	
	private void init() throws IOException{
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(executorCount,
				executorCount, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		
		this.executor = threadPoolExecutor;
		this.selectors = new SelectorThread[this.selectorCount];
		for(int i=0;i<this.selectorCount;i++){
			String selectorName = String.format("%s-%s-%d", name, selectorNamePrefix, i);
			this.selectors[i] = new SelectorThread(this, selectorName);
		}
	}
	
	/**
	 * Get selector thread by index
	 * @param index index of the selector threads array managed by this dispatcher
	 * @return corresponding SelectorThread
	 */
	public SelectorThread getSelector(int index){
		if(index <0 || index>=this.selectorCount){
			throw new IllegalArgumentException("Selector index should >=0 and <"+this.selectorCount);
		}
		return this.selectors[index];
	}
	
	/**
	 * round-robin balancing on all the selectors
	 * @return next available SelectorThread
	 */
	public SelectorThread nextSelector(){
		int nextIdx = this.selectorIndex.getAndIncrement()%this.selectorCount;
		if(nextIdx < 0){
			this.selectorIndex.set(0);
			nextIdx = 0;
		} 
		return this.selectors[nextIdx];
	}

	/**
	 * Register a channel with interestOps
	 * @param channel SelectableChannel to register
	 * @param ops interest operations(READ/WRITE) to register on the underlying channel
	 * @throws IOException if register fails
	 */
	public void registerChannel(SelectableChannel channel, int ops) throws IOException{
		this.nextSelector().registerChannel(channel, ops);
	}
	
	/**
	 * Directly register a Session with internal channel register
	 * @param ops interest operations(READ/WRITE) to register on the underlying channel
	 * @param sess Session holding the channel
	 * @throws IOException if register fails
	 */
	public void registerSession(int ops, Session sess) throws IOException{
		if(sess.selectorGroup() != this){
			throw new IOException("Unmatched Dispatcher");
		}
		this.nextSelector().registerSession(ops, sess);
	}
	
	/**
	 * Get selector thread attaching the key specified
	 * @param key SelectionKey of a channel
	 * @return selector thread attaching the key
	 */
	public SelectorThread getSelector(SelectionKey key){
		for(SelectorThread e : this.selectors){
			if(key.selector() == e.selector){
				return e;
			}
		}
		return null;
	}
	/**
	 * Start the dispatcher, which starts the underlying selector threads
	 */
	public synchronized void start() {
		if (this.started) {
			return;
		}
		try {
			this.init();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e); 
		}
		this.started = true;
		for (SelectorThread dispatcher : this.selectors) {
			dispatcher.start();
		} 
		log.info("%s(SelectorCount=%d) started", this.name, this.selectorCount);
	}
	
	/**
	 * Stop the dispatcher, interrupting the selector threads managed.
	 */
	public synchronized void stop() {
		if (!this.started)
			return;

		this.started = false;
		for (SelectorThread dispatcher : this.selectors) {
			dispatcher.interrupt();
		} 
		executor.shutdown();
		log.info("%s(SelectorCount=%d) stopped", this.name, this.selectorCount);
	} 
	 
	/**
	 * Close this dispatcher, just call the stop
	 */
	public void close() throws IOException {
		this.stop();
	}
	
	public boolean isStarted(){
		return this.started;
	}

	
	public void removeAcceptIoAdaptor(SocketAddress address){
		acceptIoAdaptors.remove(""+address);
	}
	
	public IoAdaptor ioAdaptor(SocketAddress address){
		return acceptIoAdaptors.get(""+address);
	}
	
	public ExecutorService executorService(){
		return this.executor;
	}
	
	public void asyncRun(Runnable task){
		this.executor.submit(task);
	}
	
	public int selectorCount(){
		return this.selectorCount;
	}
	
	public SelectorGroup selectorCount(int count){ 
		if(count <= 0){
			this.selectorCount = defaultSelectorCount();
		} else {
			this.selectorCount = count;
		}
		return this;
	}
	
	public static int defaultSelectorCount(){ 
		int c = Runtime.getRuntime().availableProcessors()/2;
		if(c <= 0) c = 1;
		return c;
	}
	
	public SelectorGroup executorCount(int count){ 
		if(count <= 0){
			this.executorCount = DEFAULT_EXECUTOR_COUNT;
		} else {
			this.executorCount = count;
		}
		return this;
	}
	
	public ExecutorService getExecutorService(){
		return this.executor;
	}
	
	
	public SelectorGroup name(String name){ 
		this.name = name;
		return this;
	}
	
	public ServerSocketChannel registerServerChannel(String address, IoAdaptor ioAdaptor) throws IOException{
		String[] blocks = address.split("[:]");
		if(blocks.length != 2){
			throw new IllegalArgumentException(address + " is invalid address");
		} 
		String host = blocks[0];
		int port = Integer.valueOf(blocks[1]);
		return registerServerChannel(host, port, ioAdaptor);
	}
	
	public ServerSocketChannel registerServerChannel(String host, int port, IoAdaptor ioAdaptor) throws IOException{
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    	serverSocketChannel.configureBlocking(false);
    	//serverSocketChannel.socket().setReuseAddress(true);
    	serverSocketChannel.socket().bind(new InetSocketAddress(host, port));
    	
    	acceptIoAdaptors.put(""+serverSocketChannel.socket().getLocalSocketAddress(), ioAdaptor); 
    	this.registerChannel(serverSocketChannel, SelectionKey.OP_ACCEPT); 
    	return serverSocketChannel;
	}
	
	
	public void unregisterServerChannel(ServerSocketChannel channel) throws IOException{
		removeAcceptIoAdaptor(channel.socket().getLocalSocketAddress());
		channel.close();
	}
	
	public Session registerClientChannel(String address, IoAdaptor ioAdaptor ) throws IOException{
		String[] blocks = address.split("[:]");
		if(blocks.length != 2){
			throw new IllegalArgumentException(address + " is invalid address");
		} 
		String host = blocks[0];
		int port = Integer.valueOf(blocks[1]);
		return registerClientChannel(host, port, ioAdaptor);
	} 
	
	public Session registerClientChannel(String host, int port, IoAdaptor ioAdaptor ) throws IOException{
    	Session session = createClientSession(host, port, ioAdaptor);
    	this.registerSession(SelectionKey.OP_CONNECT, session);	
    	return session;
	} 
	
	public Session createClientSession(String address, IoAdaptor ioAdaptor ) throws IOException{
		String[] blocks = address.split("[:]");
		if(blocks.length != 2){
			throw new IllegalArgumentException(address + " is invalid address");
		} 
		String host = blocks[0];
		int port = Integer.valueOf(blocks[1]);
		return createClientSession(host, port, ioAdaptor);
	} 
	
	public Session createClientSession(String host, int port, IoAdaptor ioAdaptor ) throws IOException{
		SocketChannel channel = SocketChannel.open();
    	channel.configureBlocking(false);  
    	channel.socket().setReuseAddress(true);
    	channel.connect(new InetSocketAddress(host, port)); 
    	Session session = new Session(this, channel, ioAdaptor);
    	return session;
	}
	
}
