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

import org.zbus.kit.ConfigKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.Client;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.Session;


public class DmzClient extends Client<Integer, Integer>{  
	private static final Logger log = Logger.getLogger(DmzClient.class);  
	
	private static final BindingAdaptor bindingAdaptor = new BindingAdaptor();
	
	private final Dispatcher dispatcher;  
	
	private String dmzDownAddress;   
	private String targetAddress;  
	private int reconnectTime = 1000; //ms
    
    public DmzClient(final Dispatcher dispatcher,
    		String dmzNotifyAddress,
    		String dmzDownAddress,  
    		String targetAddress){
    	super(dmzNotifyAddress, dispatcher); 
    	codec(new NotifyCodec());
    	
    	this.dispatcher = dispatcher;
    	this.dmzDownAddress = dmzDownAddress;
        this.targetAddress = targetAddress;
        
        onDisconnected(new DisconnectedHandler() {
			@Override
			public void onDisconnected() throws IOException {
				log.info("Disconnected, try to reconnect in %.1f seconds", reconnectTime/1000.0);
				try {
					Thread.sleep(reconnectTime);
				} catch (InterruptedException ex) {  
				}
				
				DmzClient.this.connectAsync();
			}
		});
        
     
    }
    
	@Override
	protected void heartbeat() {
		if(this.hasConnected()){
			try {
				send(0);
			} catch (IOException e) { 
				e.printStackTrace();
			}
		}
	}
    
	@Override 
	public void onMessage(Object obj, Session sess) throws IOException {
		Integer c = (Integer)obj; 
		if(log.isDebugEnabled()){
			log.debug("Connection requests: "+c);
		}
		Session targetSess = null, dmzDownSess = null;
		for(int i=0; i<c; i++){
			try{
				targetSess = dispatcher.createClientSession(targetAddress, bindingAdaptor);
			} catch (Exception e){
				log.error(e.getMessage() ,e);
				continue;
			}
			try{
				dmzDownSess = dispatcher.createClientSession(dmzDownAddress, bindingAdaptor);
			} catch (Exception e){
				log.error(e.getMessage() ,e);
				targetSess.close();
				continue;
			}
			
			dmzDownSess.chain = targetSess;
			targetSess.chain = dmzDownSess;
			
			dispatcher.registerSession(SelectionKey.OP_CONNECT, targetSess); 
			dispatcher.registerSession(SelectionKey.OP_CONNECT, dmzDownSess); 
		}
	}
	
	
    public void start() throws Exception{  
    	this.dispatcher.start();  
    	this.connectAsync();
    }
     
    
	public static void main(String[] args) throws Exception {  
		String dmzDown = ConfigKit.option(args, "-dmzDown", "127.0.0.1:15557");
		String dmzNotify = ConfigKit.option(args, "-dmzNotify", "127.0.0.1:15558");
		String target = ConfigKit.option(args, "-target", "10.17.2.30:3306");
		//String target = ConfigKit.option(args, "-target", "10.17.2.30:3306");
		
		final Dispatcher dispatcher = new Dispatcher();
 
		final DmzClient dmzClient = new DmzClient(dispatcher, dmzNotify, dmzDown, target);
		dmzClient.start(); 
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try {
					dmzClient.close();
					dispatcher.close();
					log.info("DmzClient shutdown completed");
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		});   
	}
}
