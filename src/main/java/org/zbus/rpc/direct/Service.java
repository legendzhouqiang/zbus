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
package org.zbus.rpc.direct;

import java.io.Closeable;
import java.io.IOException;

import org.zbus.broker.ha.ServerEntry;
import org.zbus.broker.ha.TrackPub;
import org.zbus.kit.NetKit;
import org.zbus.mq.server.UrlInfo;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.IoAdaptor;
import org.zbus.net.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.MessageServer;

public class Service implements Closeable {  
	private MessageServer server;  
	private String serverAddr;
	private final ServiceConfig config;
	private final IoAdaptor serverAdaptor;
	
	public Service(ServiceConfig config){ 
		this.config = config;   
		serverAdaptor = new DirectMessageAdaptor(config.messageProcessor); 
	}
	
	
	@Override
	public void close() throws IOException {
		 if(server != null){
			 server.close();
			 server = null;
		 }
	}
	 
	public void start() throws Exception {    
		server = new MessageServer(config.getEventDriver()); 
		server.start(config.serverHost, config.serverPort, serverAdaptor);
		
		String host = config.serverHost;
		if("0.0.0.0".equals(host)){
			host = NetKit.getLocalIp();
		}
		int port = server.getRealPort(config.serverPort);
		serverAddr = host+":"+port; 
		
		if(config.trackServerList != null){
			setupTracker();
		}  
	}
	
	
	private TrackPub trackPub; 
	private void setupTracker() {
		if(config.entryId == null){
			throw new IllegalStateException("Missing entryId for HA discovery");
		}
		trackPub = new TrackPub(config.trackServerList, server.getIoDriver());
		trackPub.onConnected(new ConnectedHandler() {
			@Override
			public void onConnected() throws IOException {
				trackPub.pubServerJoin(serverAddr); 
				ServerEntry se = new ServerEntry();
				se.entryId = config.entryId;
				se.serverAddr = serverAddr;
				se.lastUpdateTime = System.currentTimeMillis();
				se.mode = ServerEntry.RPC;
				
				trackPub.pubEntryUpdate(se);
			}
		});
		trackPub.start(); 
	}
	
	static class DirectMessageAdaptor extends MessageAdaptor {
		private final MessageProcessor processor; 
		public DirectMessageAdaptor(final MessageProcessor processor) {
			this.processor = processor;
		}
		
		private Message handleUrlMessage(Message msg, Session sess) throws IOException{
			if(msg.getBody() == null || msg.getBody().length == 0){ 
				UrlInfo url = new UrlInfo(msg.getUrl(), true); 
				
				String module = url.module == null? "" : url.module; 
				String method = url.method == null? "" : url.method;
				String json = "{";
				json += "\"module\": " + "\"" + module + "\"";
				json += ", \"method\": " + "\"" + method + "\"";
				if(url.params != null){
					json += ", \"params\": " + "[" + url.params + "]";  
				}
				json += "}";
				msg.setJsonBody(json);	
			} 
			return msg;
		}
		
		public void onSessionMessage(Object obj, Session sess) throws IOException {
			Message msg = (Message) obj;
			if(Message.HEARTBEAT.equals(msg.getCmd())){
				return;
			}
			
			final String msgId = msg.getId(); 
			msg = handleUrlMessage(msg, sess);
			if(msg == null) return;
			
			Message res = processor.process(msg);
			if (res != null) {
				res.setId(msgId);
				if (res.getStatus() == null) {
					res.setStatus(200); // default to 200
				}
				sess.write(res);
			}
		}
	}
}