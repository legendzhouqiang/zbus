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

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.kit.NetKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;

public class Server implements Closeable{
	private static final Logger log = Logger.getLogger(Server.class);  
	
	private static class IoAdaptorInfo{
		public String adaptorName;
		public String adaptorAddr;
		public ServerSocketChannel serverChannel;
		public IoAdaptor serverAdaptor;
	}
	
	protected Dispatcher dispatcher;   
	protected String serverAddr; //第一个Server注册的地址, 当侦听启动后才有效
	protected String serverName = "Server";  
	protected String serverMainIpOrder = null;
	
	protected Map<String, IoAdaptorInfo> adaptors = new ConcurrentHashMap<String, IoAdaptorInfo>();
	
	public Server(){	
	}
	
	public Server(Dispatcher dispatcher){
		this.dispatcher = dispatcher;
	}
	
	public Server(Dispatcher dispatcher, IoAdaptor serverAdaptor, int port){
		this(dispatcher, serverAdaptor, "0.0.0.0:"+port);
	}
	
	public Server(Dispatcher dispatcher, IoAdaptor serverAdaptor, String address){
		this.dispatcher = dispatcher;
		registerAdaptor(address, serverAdaptor);
	}
	
	public void registerAdaptor(int port, IoAdaptor ioAdaptor){
		registerAdaptor("0.0.0.0:"+port, ioAdaptor, null);
	}
	
	public void registerAdaptor(int port, IoAdaptor ioAdaptor, String name){
		registerAdaptor("0.0.0.0:"+port, ioAdaptor, name);
	}
	
	public void registerAdaptor(String address, IoAdaptor ioAdaptor){
		registerAdaptor(address, ioAdaptor, null);
	}

	public void registerAdaptor(String address, IoAdaptor ioAdaptor, String name){
		IoAdaptorInfo adaptor = new IoAdaptorInfo();
		String[] blocks = address.split("[:]");
		if(blocks.length != 2){
			throw new IllegalArgumentException(address + " is invalid address");
		}  
		adaptor.adaptorName = name;
		adaptor.adaptorAddr = address; 
		adaptor.serverAdaptor = ioAdaptor;  
		
		if(adaptors.isEmpty()){ //the first
			this.serverAddr = address;
		}
		
		adaptors.put(address, adaptor);
	}
	
	public void start() throws IOException{   
		if(dispatcher == null){
			throw new IllegalStateException("Missing Dispatcher");
		}
    	this.dispatcher.start();  
    	
    	for(Entry<String, IoAdaptorInfo> e : adaptors.entrySet()){ 
    		IoAdaptorInfo adaptor = e.getValue();
    		if(adaptor.serverChannel != null) continue;
    		
    		String address = adaptor.adaptorAddr; 
    		String[] bb = address.split("[:]");
    		if(bb.length !=2 ) continue;
    		String host = bb[0];
    		String port = bb[1];
    		
    		adaptor.serverChannel = dispatcher.registerServerChannel(adaptor.adaptorAddr, adaptor.serverAdaptor);
        	
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
    } 

	@Override
    public void close() throws IOException { 
		for(Entry<String, IoAdaptorInfo> e : adaptors.entrySet()){ 
    		IoAdaptorInfo adaptor = e.getValue();
    		dispatcher.unregisterServerChannel(adaptor.serverChannel);
    		adaptor.serverChannel = null;
    	}  
		adaptors.clear();
    }
    
	public void setDispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	
    public void setServerName(String serverName){
    	this.serverName = serverName;
    }  
    
	public String getServerAddr() {
		return serverAddr;
	} 
	
}
