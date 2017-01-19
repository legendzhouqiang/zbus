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
package io.zbus.mq.server;

import static io.zbus.util.ConfigUtil.isBlank;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.zbus.mq.net.MessageServer;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;
import io.zbus.util.ConfigUtil;
import io.zbus.util.NetUtil;
import io.zbus.util.logging.Logger;
import io.zbus.util.logging.LoggerFactory;

public class MqServer implements Closeable{ 
	private static final Logger log = LoggerFactory.getLogger(MqServer.class); 
	
	private final Map<String, Session> sessionTable = new ConcurrentHashMap<String, Session>();
	private final Map<String, MessageQueue> mqTable = new ConcurrentHashMap<String, MessageQueue>();
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	
	private MqServerConfig config;   
	private String serverAddr = "";    
	private EventDriver eventDriver;
	private boolean ownEventDriver = false;
	
	private MessageServer httpServer;// you may add WebSocket Server
	private MqAdaptor mqAdaptor; 
	
	public MqServer(){
		this(new MqServerConfig()); //using all defaults
	}
	
	public MqServer(MqServerConfig config){  
		this.config = config;  
		eventDriver = config.getEventDriver();
		if(eventDriver == null){
			eventDriver = new EventDriver();
			ownEventDriver = true;
		} 
		
		if(eventDriver.getSslContext() == null){
			if (!isBlank(config.sslCertificateFile) && !isBlank(config.sslPrivateKeyFile)){ 
				File sslCert = new File(config.sslCertificateFile);
				File sslPriv = new File(config.sslPrivateKeyFile);
				if(!sslCert.exists()){
					log.warn("Certificate File: " + config.sslCertificateFile + " not exists");
				}
				if(!sslPriv.exists()){
					log.warn("PrivateKey File: " + config.sslCertificateFile + " not exists");
				}
				if(sslCert.exists() && sslPriv.exists()){
					eventDriver.setSslContext(sslCert, sslPriv);
				}
			}
		}
		
		String host = config.serverHost;
		if("0.0.0.0".equals(host)){
			host = NetUtil.getLocalIp(config.serverMainIpOrder);
		}
		serverAddr = host+":"+config.serverPort; 
		
		this.scheduledExecutor.scheduleAtFixedRate(new Runnable() { 
			public void run() {  
				Iterator<Entry<String, MessageQueue>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, MessageQueue> e = iter.next();
		    		MessageQueue mq = e.getValue(); 
		    		mq.cleanSession();
		    	}
			}
		}, 1000, config.cleanMqInterval, TimeUnit.MILLISECONDS); 
		
		mqAdaptor = new MqAdaptor(this); 
		mqAdaptor.setVerbose(config.verbose);
		try {
			mqAdaptor.loadMQ();
		} catch (IOException e) {
			log.error("LoadMQ error: " + e);
		} 
	} 
	
	public void start() throws Exception{  
		long start = System.currentTimeMillis();
		httpServer = new MessageServer(eventDriver); 
		eventDriver = httpServer.getEventDriver();
		
		httpServer.start(config.serverHost, config.serverPort, mqAdaptor);  
		 
		long end = System.currentTimeMillis();
		log.info("Zbus started sucessfully in %d ms", (end-start));
		
	}
	 
	@Override
	public void close() throws IOException {   
		scheduledExecutor.shutdown();   
		mqAdaptor.close();
		if(httpServer != null){
			httpServer.close();
		} 
		if(ownEventDriver && eventDriver != null){
			eventDriver.close();
			eventDriver = null;
		}
	} 
     
    public void pubEntryUpdate(MessageQueue mq){
    	 //TODO
    }  
    
	public Map<String, MessageQueue> getMqTable() {
		return mqTable;
	} 

	public Map<String, Session> getSessionTable() {
		return sessionTable;
	}  

	public String getServerAddr() {
		return serverAddr;
	}  

	public MqServerConfig getConfig() {
		return config;
	}   
	
	public MqAdaptor getMqAdaptor() {
		return mqAdaptor;
	}
	
	public EventDriver getEventDriver() {
		return eventDriver;
	}

	public static void main(String[] args) throws Exception {
		MqServerConfig config = new MqServerConfig(); 
		String xmlConfigFile = ConfigUtil.option(args, "-conf", "conf/zbus.xml");
		try{
			config.loadFromXml(xmlConfigFile); 
		} catch(Exception ex){ 
			String message = xmlConfigFile + " config error encountered\n" + ex.getMessage();
			System.err.println(message);
			log.warn(message); 
			return;
		} 
		
		final MqServer server = new MqServer(config);
		server.start(); 
		Runtime.getRuntime().addShutdownHook(new Thread(){ 
			public void run() { 
				try { 
					server.close();
					log.info("MqServer shutdown completed");
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		});    
	} 
}


