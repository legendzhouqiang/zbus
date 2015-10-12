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
import java.io.InputStream;
import java.nio.channels.SelectionKey;
import java.util.Properties;

import org.zbus.kit.ConfigKit;
import org.zbus.kit.FileKit;
import org.zbus.kit.NetKit;
import org.zbus.kit.log.Logger;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
 
public class TcpProxyServer extends BindingAdaptor{ 
	private static final Logger log = Logger.getLogger(TcpProxyServer.class);   
	
	private String targetAddress;   
	
    public TcpProxyServer(String targetAddress){ 
    	codec(new ProxyCodec());  
        this.targetAddress = targetAddress;
    } 
    
    @Override
    protected void onSessionAccepted(Session sess) throws IOException {
    	log.info("Accepted: %s", sess);   
    	Session target = null;
    	Dispatcher dispatcher = sess.getDispatcher();
    	try{ 
	    	target = dispatcher.createClientSession(targetAddress, this); 
    	} catch (Exception e){ 
    		log.error("Reject upstream connection: %s", sess);
    		log.error(e.getMessage(), e);
    		
    		sess.asyncClose();
    		return;
    	}
    	
    	sess.chain = target;
    	target.chain = sess; 
    	dispatcher.registerSession(SelectionKey.OP_CONNECT, target);  
    } 
    
	public static void main(String[] args) throws Exception {   
		String conf = ConfigKit.option(args, "-conf", "proxy.properties");
		
		InputStream inputStream = FileKit.loadFile(conf);
		if(inputStream == null){ 
			log.error("Missing configure file(%s)", conf);
			return;
		}
		Properties props = new Properties();
		props.load(inputStream); 
		
		Dispatcher dispatcher = new Dispatcher();
		final Server server = new Server(dispatcher);
		for(Object key : props.keySet()){
			String sKey = (String)key;
			if(!sKey.startsWith("tcp.")) continue;
			String listenPort = sKey.substring(4);
			int port;
			try{
				port = Integer.valueOf(listenPort);
			}catch(Exception ex){
				continue;
			}
			String target = props.getProperty(sKey);
			IoAdaptor ioAdaptor = new TcpProxyServer(target);
			server.registerAdaptor(port, ioAdaptor);
			log.info("%s:%d====>%s", NetKit.getLocalIp(), port, target);
		}
	
		server.setServerName("TcpProxyServer");
		server.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try {
					server.close();
					log.info("DmzServer shutdown completed");
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}); 
	}
}
