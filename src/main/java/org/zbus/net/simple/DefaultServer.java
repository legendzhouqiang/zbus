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

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.kit.NetKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.CodecInitializer;
import org.zbus.net.EventDriver;
import org.zbus.net.IoAdaptor;
import org.zbus.net.Server;

/**
 * A Server is just the utility functionality of SelectorGroup which manages multiple server side
 * IoAdaptors, nothing else special
 * 
 * Callers could new a Server with a SelectorGroup and start all stuff together to quickly generate
 * a socket server.
 * 
 * @author rushmore (洪磊明)
 *
 */
public class DefaultServer implements Server{
	private static final Logger log = Logger.getLogger(DefaultServer.class);  
	private Codec codec;
	private static class IoAdaptorInfo{
		public String adaptorName;
		public String adaptorAddr;
		public ServerSocketChannel serverChannel;
		public IoAdaptor serverAdaptor;
	}
	
	protected SelectorGroup selectorGroup;   
	protected String serverAddr; //Server address = address of the first registered IoAdaptor
	protected String serverName = "Server";  
	protected String serverMainIpOrder = null; 
	
	protected Map<String, IoAdaptorInfo> adaptors = new ConcurrentHashMap<String, IoAdaptorInfo>();
	protected EventDriver eventDriver;
	protected boolean ownEventDriver = false;
	
	public DefaultServer(){
		this(new EventDriver(true));
		this.ownEventDriver = true;
	}
	
	public DefaultServer(EventDriver driver){
		driver.validateDefault();
		
		this.eventDriver = driver;
		this.selectorGroup = (SelectorGroup)driver.getGroup();
		this.selectorGroup.start();  
	}  
	
	@Override
	public void start(int port, IoAdaptor ioAdaptor) throws Exception {
		start("0.0.0.0", port, ioAdaptor);
	}
	
	@Override
	public void join() throws InterruptedException {
		while(true){
			Thread.sleep(10000);
		}
	} 
	
	public void start(String host, int port, IoAdaptor ioAdaptor) throws Exception{
		init();
		
		IoAdaptorInfo adaptor = new IoAdaptorInfo();
		String address = host + ":" + port;
		adaptors.put(address, adaptor);
		
		adaptor.adaptorName = "";
		adaptor.adaptorAddr = address; 
		adaptor.serverAdaptor = ioAdaptor;  
    	adaptor.serverChannel = selectorGroup.registerServerChannel(adaptor.adaptorAddr, 
    				adaptor.serverAdaptor, codec);
        	
		ServerSocket ss = adaptor.serverChannel.socket();
		if(address.equals(serverAddr)){ 
    		String localHost = host;
    		if("0.0.0.0".equals(host)){
    			if(serverMainIpOrder == null){
    				localHost = NetKit.getLocalIp();
    			} else {
    				localHost = NetKit.getLocalIp(serverMainIpOrder);
    			}
    		}
    		serverAddr = String.format("%s:%d", localHost, ss.getLocalPort());
    	} 
		if("0".equals(port)){
			adaptor.adaptorAddr = String.format("%s:%d", host, ss.getLocalPort());
		} 
		
		String info = String.format("%s-%s listening [%s]", this.serverName, adaptor.adaptorName, adaptor.adaptorAddr);
		if(adaptor.adaptorName == null){
			info = String.format("%s listening [%s]", this.serverName, adaptor.adaptorAddr);
		}
    	log.info(info); 
    } 
 
    public void close() throws IOException { 
		for(Entry<String, IoAdaptorInfo> e : adaptors.entrySet()){ 
    		IoAdaptorInfo adaptor = e.getValue();
    		if(selectorGroup == null) continue; //
    		if(adaptor.serverChannel != null){
    			selectorGroup.unregisterServerChannel(adaptor.serverChannel);
    		}
    		adaptor.serverChannel = null;
    	}  
		adaptors.clear(); 
		
		if(ownEventDriver && eventDriver != null){
			eventDriver.close();
			eventDriver = null;
		}
    }   
	
	private void init(){
		if(codec != null) return;
		if(codecInitializer == null){
			throw new IllegalStateException("Missing codecInitializer");
		}
		List<Object> handlers = new ArrayList<Object>();
		codecInitializer.initPipeline(handlers);
		for(Object handler : handlers){
			if(!(handler instanceof Codec)){
				throw new IllegalArgumentException("Invalid ChannelHandler: " + handler);
			} 
			this.codec = (Codec)handler;
		} 
	}
	protected CodecInitializer codecInitializer; 
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	} 
	
	public EventDriver getEventDriver() {
		return this.eventDriver;
	}
}
