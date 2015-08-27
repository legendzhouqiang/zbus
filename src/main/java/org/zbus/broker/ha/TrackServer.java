package org.zbus.broker.ha;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.log.Logger;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.net.http.MessageAdaptor;

import com.alibaba.fastjson.JSON;

class MultiBrokerEntrySet{
	//entry_id ==> list of same entries from different brokers
	Map<String, Set<BrokerEntry>> entryId2EntrySet = new ConcurrentHashMap<String, Set<BrokerEntry>>();
	//broker_addr ==> list of entries from same broker
	Map<String, Set<BrokerEntry>> broker2EntrySet = new ConcurrentHashMap<String, Set<BrokerEntry>>();
	
	public boolean isNewBroker(BrokerEntry be){
		return !broker2EntrySet.containsKey(be.getBroker());
	}
	
	public void updateBrokerEntry(BrokerEntry be){
		String entryId = be.getId(); 
		Set<BrokerEntry> entries = entryId2EntrySet.get(entryId);
		if(entries == null){
			entries = Collections.synchronizedSet(new HashSet<BrokerEntry>());
			entryId2EntrySet.put(entryId, entries);
		}
		//update
		entries.remove(be);
		entries.add(be);
		 
		String brokerAddr = be.getBroker();
		entries = broker2EntrySet.get(brokerAddr);
		if(entries == null){
			entries = Collections.synchronizedSet(new HashSet<BrokerEntry>());
			broker2EntrySet.put(brokerAddr, entries); 
		}
		entries.remove(be);
		entries.add(be); 
	}
	
	public void removeBroker(String broker){
		Set<BrokerEntry> brokerEntries = broker2EntrySet.get(broker);
		if(brokerEntries == null) return;
		for(BrokerEntry be : brokerEntries){
			Set<BrokerEntry> entryOfId = entryId2EntrySet.get(be.getId());
			if(entryOfId == null) continue; 
			entryOfId.remove(be);
		}
	}
	
	public Map<String, Set<BrokerEntry>> entryTable(){
		return entryId2EntrySet;
	}
}


public class TrackServer extends MessageAdaptor{
	static final Logger log = Logger.getLogger(TrackServer.class); 
	MultiBrokerEntrySet brokerEntrySet = new MultiBrokerEntrySet(); 
	
	Map<String, Session> subClientSessions =  new ConcurrentHashMap<String, Session>();
	Map<String, Session> brokerHeartbeatSessions = new ConcurrentHashMap<String, Session>();
	
	public TrackServer(){  
		cmd(Protocol.EntryUpdate, updateHandler); 
		cmd(Protocol.EntryQueryAll, queryAllHandler);
		cmd(Protocol.EntrySub, subHandler);
	}  
	
	private BrokerEntry parseBrokerEntry(Message msg){
		return JSON.parseObject(msg.getBodyString(), BrokerEntry.class);
	} 
	private String brokerEntryTableJson(){
		return JSON.toJSONString(brokerEntrySet.entryTable());
	}
	
	private void onNewBroker(final String brokerAddr, Session sess) throws IOException{
		
		sess.dispatcher().createClientSession(brokerAddr, new MessageAdaptor() {
			private void cleanSession(Session sess){
				brokerHeartbeatSessions.remove(sess.id());
				String broker = sess.attr("broker");
				if(broker == null) return;
				brokerEntrySet.removeBroker(broker);
			}
			
			@Override
			protected void onSessionConnected(Session sess) throws IOException { 
				brokerHeartbeatSessions.put(sess.id(), sess);
				sess.attr("broker", brokerAddr);
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
	
	private MessageHandler updateHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			BrokerEntry be = null;
			try{
				be = parseBrokerEntry(msg);
			} catch(Exception e){
				log.error(e.getMessage(), e);
				return;
			}
			boolean isNewBroker = brokerEntrySet.isNewBroker(be);
			
			brokerEntrySet.updateBrokerEntry(be);
			if(isNewBroker){
				onNewBroker(be.getBroker(), sess);
			}
			
			//real-time updating to clients
			Iterator<Entry<String, Session>> iter = subClientSessions.entrySet().iterator();
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
	}; 
	
	private MessageHandler queryAllHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setId(msg.getId());
			res.setBody(brokerEntryTableJson());
			sess.write(res);
		}
	};
	
	private MessageHandler subHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			subClientSessions.put(sess.id(), sess);
		}
	};
	
	protected void onSessionDestroyed(Session sess) throws IOException {
		subClientSessions.remove(sess); 
	}
	
	@Override
	protected void onException(Throwable e, Session sess) throws IOException {
		subClientSessions.remove(sess);
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
