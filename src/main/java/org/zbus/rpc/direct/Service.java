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

import java.io.IOException;

import org.zbus.broker.ha.ServerEntry;
import org.zbus.broker.ha.TrackPub;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageProcessor;
import org.zbus.net.http.MessageCodec;
import org.zbus.proxy.thrift.ThriftCodec;

public class Service extends Server {  
	private boolean ownDispatcher = false;
	private final String trackServerList;
	private final String entryId;
	
	public Service(ServiceConfig config){ 
		serverName = "RpcServer";  
		trackServerList = config.trackServerList;
		entryId = config.entryId;
		
		if(config.dispatcher != null){
			dispatcher = config.dispatcher;
		} else {
			dispatcher = new Dispatcher()
				.selectorCount(config.selectorCount)
				.executorCount(config.executorCount);
			ownDispatcher = true;
		}
		
		IoAdaptor serverAdaptor = new DirectMessageAdaptor(config.messageProcessor);
		String address = config.serverHost+":"+config.serverPort;
		registerAdaptor(address, serverAdaptor);
		
		if(config.thriftServer != null){
			DirectMessageAdaptor thriftAdaptor = new DirectMessageAdaptor(config.messageProcessor);
			thriftAdaptor.codec(new ThriftCodec()); 
			registerAdaptor(config.thriftServer, thriftAdaptor);
		}
	}
	
	@Override
	public void close() throws IOException {
		super.close(); 
		if(ownDispatcher){
			dispatcher.close();
		}
	}
	
	@Override
	public void start() throws IOException { 
		super.start(); 
		
		if(trackServerList != null){
			setupTracker();
		}
	}
	
	
	private TrackPub trackPub; 
	private void setupTracker() {
		if(entryId == null){
			throw new IllegalStateException("Missing entryId for HA discovery");
		}
		trackPub = new TrackPub(trackServerList, dispatcher);
		trackPub.onConnected(new ConnectedHandler() {
			@Override
			public void onConnected(Session sess) throws IOException {
				trackPub.pubServerJoin(serverAddr); 
				ServerEntry se = new ServerEntry();
				se.entryId = entryId;
				se.serverAddr = serverAddr;
				se.lastUpdateTime = System.currentTimeMillis();
				se.mode = ServerEntry.RPC;
				
				trackPub.pubEntryUpdate(se);
			}
		});
		trackPub.start(); 
	}
	
	static class DirectMessageAdaptor extends IoAdaptor {
		private final MessageProcessor processor;

		public DirectMessageAdaptor(final MessageProcessor processor) {
			codec(new MessageCodec());
			this.processor = processor;
		}

		protected void onMessage(Object obj, Session sess) throws IOException {
			Message msg = (Message) obj;
			final String msgId = msg.getId();
			Message res = processor.process(msg);
			if (res != null) {
				res.setId(msgId);
				if (res.getResponseStatus() == null) {
					res.setResponseStatus(200); // default to 200
				}
				sess.write(res);
			}
		}
	}
}