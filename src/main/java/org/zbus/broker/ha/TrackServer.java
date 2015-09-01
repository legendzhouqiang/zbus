package org.zbus.broker.ha;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.log.Logger;
import org.zbus.net.Client.ConnectedHandler;
import org.zbus.net.Client.ErrorHandler;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageAdaptor;
import org.zbus.net.http.MessageClient;

public class TrackServer extends MessageAdaptor implements Closeable{
	private static final Logger log = Logger.getLogger(TrackServer.class); 
	private final ServerEntryTable serverEntryTable = new ServerEntryTable(); 
	
	private Map<String, Session> trackSubs =  new ConcurrentHashMap<String, Session>();
	private Map<String, MessageClient> joinedServers = new ConcurrentHashMap<String, MessageClient>();
	
	public TrackServer(){  
		cmd(HaCommand.EntryUpdate, entryUpdateHandler); 
		cmd(HaCommand.EntryRemove, entryRemoveHandler); 

		cmd(HaCommand.ServerJoin, serverJoinHandler);
		cmd(HaCommand.ServerLeave, serverLeaveHandler);
		
		cmd(HaCommand.PubAll, pubAllHandler);
		cmd(HaCommand.SubAll, subAllHandler);
	}   
	
	private void pubMessage(Message msg){
		Iterator<Entry<String, Session>> iter = trackSubs.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, Session> entry = iter.next();
			Session sub = entry.getValue();
			try{
				msg.removeHead(Message.ID);
				sub.write(msg);
			}catch(Exception e){
				iter.remove();
				log.error(e.getMessage(), e);
			}
		}
	}
	
	private MessageHandler serverJoinHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			String serverAddr = msg.getServer();
			if(serverAddr == null) return;
			onNewServer(serverAddr, sess);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler serverLeaveHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			String serverAddr = msg.getServer();
			if(serverAddr == null) return; 
			serverEntryTable.removeServer(serverAddr);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler entryUpdateHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			ServerEntry se = null;
			try{
				se = ServerEntry.parseJson(msg.getBodyString());
			} catch(Exception e){
				log.error(e.getMessage(), e);
				return;
			}
			boolean isNewServer = serverEntryTable.isNewServer(se);
			
			serverEntryTable.updateServerEntry(se);
			if(isNewServer){
				onNewServer(se.serverAddr, sess);
			}
			
			pubMessage(msg);
		}
	}; 
	
	private MessageHandler entryRemoveHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			String serverAddr = msg.getServer();
			String entryId = msg.getMq(); //use mq for entryId
			serverEntryTable.removeServerEntry(serverAddr, entryId);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler pubAllHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			
		}
	};
	
	private MessageHandler subAllHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			log.info("%s", msg);
			trackSubs.put(sess.id(), sess); 
			msg.setCmd(HaCommand.PubAll);
			msg.setBody(serverEntryTable.toJsonString());
			sess.write(msg);
		}
	};
	
	private void onNewServer(final String serverAddr, Session sess) throws IOException{
		
		synchronized (joinedServers) {
			if(joinedServers.containsKey(serverAddr)) return; 
			 
			log.info(">>>>>>>>>>>New Server: "+ serverAddr);
			final MessageClient client = new MessageClient(serverAddr, sess.dispatcher()); 
			joinedServers.put(serverAddr, client); 
			
			client.onConnected(new ConnectedHandler() { 
				@Override
				public void onConnected(Session sess) throws IOException { 
					joinedServers.put(serverAddr, client); 
				}
			});
			
			client.onError(new ErrorHandler() { 
				@Override
				public void onError(IOException e, Session sess) throws IOException { 
					log.warn("Server(%s) down", serverAddr);
					joinedServers.remove(serverAddr); 
					serverEntryTable.removeServer(serverAddr);  
					client.close();
					
					log.info("Sending ServerLeave message");
					Message msg = new Message();
					msg.setCmd(HaCommand.ServerLeave);
					msg.setServer(serverAddr);
					
					pubMessage(msg);
				}
			}); 
			
			client.connectAsync(); 
		}
	}
	
	protected void onSessionDestroyed(Session sess) throws IOException {
		trackSubs.remove(sess.id()); 
	}
	
	@Override
	protected void onException(Throwable e, Session sess) throws IOException {
		trackSubs.remove(sess.id());
		super.onException(e, sess);
	}
	
	@Override
	public void close() throws IOException { 
		serverEntryTable.close(); 
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
