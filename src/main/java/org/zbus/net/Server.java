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
import java.nio.channels.ServerSocketChannel;

import org.zbus.kit.NetKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;

public class Server implements Closeable{
	private static final Logger log = Logger.getLogger(Server.class);  
	
	protected Dispatcher dispatcher; 
	protected String host = "0.0.0.0";
	protected int port = 80;
	
	protected String serverAddr = host+":"+port;
	protected String serverName = "Server";
	protected ServerSocketChannel serverChannel;
	
	protected IoAdaptor serverAdaptor;  
	
	public Server(String address){
		this(null, null, address);
	}
	
	public Server(Dispatcher dispatcher, IoAdaptor serverAdaptor, int port){
		this(dispatcher, serverAdaptor, "0.0.0.0:"+port);
	}
	
	public Server(Dispatcher dispatcher, IoAdaptor serverAdaptor, String address){
		this.dispatcher = dispatcher;
		this.serverAdaptor = serverAdaptor;
		String[] blocks = address.split("[:]");
		if(blocks.length != 2){
			throw new IllegalArgumentException(address + " is invalid address");
		} 
		this.host = blocks[0];
		this.port = Integer.valueOf(blocks[1]);
		
		if("0.0.0.0".equals(host)){
			serverAddr = String.format("%s:%d",NetKit.getLocalIp(), port);
		}
	}
	
	public void start() throws IOException{  
		if(serverAdaptor == null){
			throw new IllegalStateException("Missing serverIoAdaptor");
		}
    	if(serverChannel != null){
    		log.info("Server already started");
    		return;
    	}
    	this.dispatcher.start(); 
    	
    	serverChannel = dispatcher.registerServerChannel(host, port, serverAdaptor);
    	log.info("%s listening [%s:%d]", this.serverName, host, port);
    } 

	@Override
    public void close() throws IOException { 
    	if(serverChannel != null){
    		serverChannel.close();
    		dispatcher.unregisterServerChannel(serverChannel);
    	}
    }
    
	public void setDispatcher(Dispatcher dispatcher) {
		this.dispatcher = dispatcher;
	}
	
    public void setServerName(String serverName){
    	this.serverName = serverName;
    } 

	public void setServerAdaptor(IoAdaptor serverAdaptor) {
		this.serverAdaptor = serverAdaptor;
	}

	public String getServerAddr() {
		return serverAddr;
	} 
	
}
