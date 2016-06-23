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
package org.zbus.mq.server;

import static org.zbus.kit.ConfigKit.isBlank;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.zbus.broker.BrokerConfig;
import org.zbus.broker.ha.ServerEntry;
import org.zbus.broker.ha.TrackPub;
import org.zbus.kit.ClassKit;
import org.zbus.kit.ConfigKit;
import org.zbus.kit.NetKit;
import org.zbus.kit.log.Logger;
import org.zbus.kit.log.LoggerFactory;
import org.zbus.mq.Protocol.MqInfo;
import org.zbus.mq.server.filter.MemoryMqFilter;
import org.zbus.mq.server.filter.MqFilter;
import org.zbus.mq.server.filter.PersistMqFilter;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.EventDriver;
import org.zbus.net.Session;
import org.zbus.net.http.MessageServer;
import org.zbus.proxy.HttpDmzProxy;
import org.zbus.proxy.HttpDmzProxy.ProxyConfig;

public class MqServer implements Closeable{ 
	private static final Logger log = LoggerFactory.getLogger(MqServer.class); 
	
	private final Map<String, Session> sessionTable = new ConcurrentHashMap<String, Session>();
	private final Map<String, AbstractMQ> mqTable = new ConcurrentHashMap<String, AbstractMQ>();
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	
	private MqServerConfig config; 
	
	public void setConfig(MqServerConfig config)
    {
        this.config = config;
    }

    private MqFilter mqFilter; 
	private String serverAddr = "";  
	
	private TrackPub trackPub; 
	
	private EventDriver eventDriver;
	private boolean ownEventDriver = false;
	
	private MessageServer httpServer;// you may add WebSocket Server
	private MqAdaptor mqAdaptor;
	private List<HttpDmzProxy> httpDmzProxies = new ArrayList<HttpDmzProxy>();
	
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
			host = NetKit.getLocalIp(config.serverMainIpOrder);
		}
		serverAddr = host+":"+config.serverPort;
		
		if(config.mqFilterPersist){
			if(ClassKit.bdbAvailable){
				mqFilter = new PersistMqFilter(config.storePath + File.separator + "filter");
			} else {
				log.warn("MqFilter persist mode enabled, but missing je-5.0.xx jar, default to MemoryMqFilter");	 
			}  
		}  else {
			mqFilter = new MemoryMqFilter();
		}
		
		this.scheduledExecutor.scheduleAtFixedRate(new Runnable() { 
			public void run() {  
				Iterator<Entry<String, AbstractMQ>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, AbstractMQ> e = iter.next();
		    		AbstractMQ mq = e.getValue(); 
		    		mq.cleanSession();
		    	}
			}
		}, 1000, config.cleanMqInterval, TimeUnit.MILLISECONDS); 
		
		mqAdaptor = new MqAdaptor(this); 
		mqAdaptor.setVerbose(config.verbose);
		mqAdaptor.loadMQ(); 
	} 
	
	public void start() throws Exception{  
		long start = System.currentTimeMillis();
		httpServer = new MessageServer(eventDriver); 
		eventDriver = httpServer.getEventDriver();
		
		httpServer.start(config.serverHost, config.serverPort, mqAdaptor); 
		
		if(config.trackServerList != null && !"".equals(config.trackServerList)){
			log.info("Zbus run in HA mode");
			setupTracker(config.trackServerList);
		}
		
		if(config.getHttpProxyConfigList() != null){
			for(ProxyConfig proxyConfig: config.getHttpProxyConfigList()){
				BrokerConfig brokerConfig = new BrokerConfig();
				brokerConfig.brokerAddress = null;//indicates JVM broker
				brokerConfig.mqServer = this;
				proxyConfig.brokerConfig = brokerConfig;
				
				HttpDmzProxy proxy = new HttpDmzProxy(proxyConfig);
				httpDmzProxies.add(proxy);
				proxy.start();
			}
		}
		long end = System.currentTimeMillis();
		log.info("Zbus started sucessfully in %d ms", (end-start));
		
	}
	 
	@Override
	public void close() throws IOException { 
		if(this.mqFilter != null){
			this.mqFilter.close();
		}
		if(trackPub != null){
    		trackPub.close();
    	}
		scheduledExecutor.shutdown();  
		for(HttpDmzProxy proxy : httpDmzProxies){
			proxy.close();
		}
		mqAdaptor.close();
		if(httpServer != null){
			httpServer.close();
		} 
		if(ownEventDriver && eventDriver != null){
			eventDriver.close();
			eventDriver = null;
		}
	}
	
	
    
	public void setupTracker(String trackServerList){
		if(eventDriver == null){
			throw new IllegalStateException("Missing eventDriver");
		}
    	trackPub = new TrackPub(trackServerList, eventDriver);
    	trackPub.onConnected(new ConnectedHandler() {
    		@Override
    		public void onConnected() throws IOException { 
    			trackPub.pubServerJoin(serverAddr);
    			for(AbstractMQ mq : mqTable.values()){
    				pubEntryUpdate(mq);
    			}
    		}
		});
    	trackPub.start();
    	
    	scheduledExecutor.scheduleAtFixedRate(new Runnable() { 
			@Override
			public void run() { 
				for(AbstractMQ mq : mqTable.values()){
    				pubEntryUpdate(mq);
    			}
			}
		}, 2000, config.trackReportInterval, TimeUnit.MILLISECONDS);
    }  
     
    public void pubEntryUpdate(AbstractMQ mq){
    	if(trackPub == null) return;
    	 
    	MqInfo info = mq.getMqInfo();
    	
    	ServerEntry se = new ServerEntry();
    	se.entryId = info.name;
    	se.serverAddr = serverAddr;
    	se.consumerCount = info.consumerCount;
    	se.mode = info.mode;
    	se.unconsumedMsgCount = info.unconsumedMsgCount;
    	se.lastUpdateTime = mq.lastUpdateTime;

    	trackPub.pubEntryUpdate(se);
    }  
    
	public Map<String, AbstractMQ> getMqTable() {
		return mqTable;
	} 

	public Map<String, Session> getSessionTable() {
		return sessionTable;
	}  

	public String getServerAddr() {
		return serverAddr;
	} 

	public MqFilter getMqFilter() {
		return mqFilter;
	}  

	public MqServerConfig getConfig() {
		return config;
	}   
	
	public MqAdaptor getMqAdaptor() {
		return mqAdaptor;
	}

	public static void main(String[] args) throws Exception {
		MqServerConfig config = new MqServerConfig(); 
		String xmlConfigFile = ConfigKit.option(args, "-conf", "conf/zbus.xml");
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


