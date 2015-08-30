package org.zbus.broker.ha;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.broker.ha.ServerEntry.HaServerEntrySet;
import org.zbus.log.Logger;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageAdaptor;

public class TrackServer extends MessageAdaptor{
	private static final Logger log = Logger.getLogger(TrackServer.class); 
	private HaServerEntrySet haServerEntrySet = new HaServerEntrySet(); 
	
	private Map<String, Session> trackSubs =  new ConcurrentHashMap<String, Session>();
	private Map<String, Session> targetServers = new ConcurrentHashMap<String, Session>();
	
	public TrackServer(){  
		cmd(HaCommand.EntryUpdate, entryUpdateHandler); 
		cmd(HaCommand.EntryRemove, entryRemoveHandler); 

		cmd(HaCommand.TargetJoin, targetJoinHandler);
		cmd(HaCommand.TargetLeave, targetLeaveHandler);
		
		cmd(HaCommand.PubAll, pubAllHandler);
		cmd(HaCommand.SubAll, subAllHandler);
	}   
	
	private void pubMessage(Message msg){
		Iterator<Entry<String, Session>> iter = trackSubs.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, Session> entry = iter.next();
			Session sub = entry.getValue();
			try{
				sub.write(msg);
			}catch(Exception e){
				iter.remove();
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private MessageHandler targetJoinHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			String serverAddr = msg.getServer();
			if(serverAddr == null) return;
			onNewServer(serverAddr, sess);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler targetLeaveHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			String serverAddr = msg.getServer();
			if(serverAddr == null) return; 
			haServerEntrySet.removeServer(serverAddr);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler entryUpdateHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			ServerEntry be = null;
			try{
				be = ServerEntry.parseJson(msg.getBodyString());
			} catch(Exception e){
				log.error(e.getMessage(), e);
				return;
			}
			boolean isNewServer = haServerEntrySet.isNewServer(be);
			
			haServerEntrySet.updateServerEntry(be);
			if(isNewServer){
				onNewServer(be.getServerAddr(), sess);
			}
			
			pubMessage(msg);
		}
	}; 
	
	private MessageHandler entryRemoveHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			String serverAddr = msg.getServer();
			String entryId = msg.getMq(); //use mq for entryId
			haServerEntrySet.removeServerEntry(serverAddr, entryId);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler pubAllHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setId(msg.getId());
			res.setBody(haServerEntrySet.toJsonString());
			sess.write(res);
		}
	};
	
	private MessageHandler subAllHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			log.info("%s", msg);
			trackSubs.put(sess.id(), sess);
			List<ServerEntry> entries = haServerEntrySet.getAllServerEntries();
			for(ServerEntry be : entries){ 
				msg.setCmd(HaCommand.EntryUpdate);
				msg.setBody(be.toJsonString());  
				
				sess.write(msg);
			}
		}
	};
	
	private void connectToNewServer(final String serverAddr, Session sess) throws IOException{
		Dispatcher dispatcher = sess.dispatcher();
		dispatcher.createClientSession(serverAddr, new MessageAdaptor() {
			private void cleanSession(Session sess){
				log.info("Sending ServerLeave Message>>>");
				targetServers.remove(sess.id());
				String serverAddr = sess.attr("server");
				if(serverAddr == null) return;
				haServerEntrySet.removeServer(serverAddr);
				
				
				Message msg = new Message();
				msg.setCmd(HaCommand.TargetLeave);
				msg.setServer(serverAddr);
				
				pubMessage(msg);
			}
			
			@Override
			protected void onSessionConnected(Session sess) throws IOException { 
				targetServers.put(sess.id(), sess);
				sess.attr("server", serverAddr);
				super.onSessionConnected(sess);
			}
			@Override
			protected void onException(Throwable e, Session sess)
					throws IOException { 
				cleanSession(sess);
				super.onException(e, sess);
			}
			
			@Override
			protected void onSessionDestroyed(Session sess) throws IOException {
				cleanSession(sess);
				super.onSessionDestroyed(sess);
			} 
		}); 
	}
	
	private void onNewServer(final String serverAddr, Session sess) throws IOException{
		synchronized (targetServers) {
			if(targetServers.containsKey(serverAddr)) return;
			targetServers.put(serverAddr, null);
			connectToNewServer(serverAddr, sess);
		} 
	}
	
	protected void onSessionDestroyed(Session sess) throws IOException {
		trackSubs.remove(sess); 
	}
	
	@Override
	protected void onException(Throwable e, Session sess) throws IOException {
		trackSubs.remove(sess);
		super.onException(e, sess);
	}

	public static void main(String[] args) throws Exception { 
		Dispatcher dispatcher = new Dispatcher();
		IoAdaptor ioAdaptor = new TrackServer();
		
		@SuppressWarnings("resource")
		Server server = new Server(dispatcher, ioAdaptor, 16666);
		server.setServerName("TrackServer");
		server.start();
	}  
}
