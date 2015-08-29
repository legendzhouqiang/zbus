package org.zbus.broker.ha;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.zbus.broker.ha.BrokerEntry.HaBrokerEntrySet;
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
	private HaBrokerEntrySet haBrokerEntrySet = new HaBrokerEntrySet(); 
	
	private Map<String, Session> subscribers =  new ConcurrentHashMap<String, Session>();
	private Map<String, Session> brokers = new ConcurrentHashMap<String, Session>();
	
	public TrackServer(){  
		cmd(HaCommand.EntryUpdate, entryUpdateHandler); 
		cmd(HaCommand.EntryRemove, entryRemoveHandler); 

		cmd(HaCommand.BrokerJoin, brokerJoinHandler);
		cmd(HaCommand.BrokerLeave, brokerLeaveHandler);
		
		cmd(HaCommand.QueryAll, queryAllHandler);
		cmd(HaCommand.Subscribe, subscribeHandler);
	}   
	
	private void pubMessage(Message msg){
		Iterator<Entry<String, Session>> iter = subscribers.entrySet().iterator();
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
	
	private MessageHandler brokerJoinHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			String broker = msg.getBroker();
			if(broker == null) return;
			onNewBroker(broker, sess);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler brokerLeaveHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			String broker = msg.getBroker();
			if(broker == null) return; 
			haBrokerEntrySet.removeBroker(broker);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler entryUpdateHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException { 
			log.info("%s", msg);
			BrokerEntry be = null;
			try{
				be = BrokerEntry.parseJson(msg.getBodyString());
			} catch(Exception e){
				log.error(e.getMessage(), e);
				return;
			}
			boolean isNewBroker = haBrokerEntrySet.isNewBroker(be);
			
			haBrokerEntrySet.updateBrokerEntry(be);
			if(isNewBroker){
				onNewBroker(be.getBroker(), sess);
			}
			
			pubMessage(msg);
		}
	}; 
	
	private MessageHandler entryRemoveHandler = new MessageHandler() { 
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			String broker = msg.getBroker();
			String entryId = msg.getMq(); //use mq for entryId
			haBrokerEntrySet.removeBrokerEntry(broker, entryId);
			
			pubMessage(msg);
		}
	};
	
	private MessageHandler queryAllHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setId(msg.getId());
			res.setBody(haBrokerEntrySet.toJsonString());
			sess.write(res);
		}
	};
	
	private MessageHandler subscribeHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			log.info("%s", msg);
			subscribers.put(sess.id(), sess);
			List<BrokerEntry> entries = haBrokerEntrySet.getAllBrokerEntries();
			for(BrokerEntry be : entries){ 
				msg.setCmd(HaCommand.EntryUpdate);
				msg.setBody(be.toJsonString());  
				
				sess.write(msg);
			}
		}
	};
	
	private void connectToNewBroker(final String brokerAddr, Session sess) throws IOException{
		Dispatcher dispatcher = sess.dispatcher();
		dispatcher.createClientSession(brokerAddr, new MessageAdaptor() {
			private void cleanSession(Session sess){
				log.info("Sending BrokerLeave Message>>>");
				brokers.remove(sess.id());
				String broker = sess.attr("broker");
				if(broker == null) return;
				haBrokerEntrySet.removeBroker(broker);
				
				
				Message msg = new Message();
				msg.setCmd(HaCommand.BrokerLeave);
				msg.setBroker(broker);
				
				pubMessage(msg);
			}
			
			@Override
			protected void onSessionConnected(Session sess) throws IOException { 
				brokers.put(sess.id(), sess);
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
	
	private void onNewBroker(final String brokerAddr, Session sess) throws IOException{
		synchronized (brokers) {
			if(brokers.containsKey(brokerAddr)) return;
			brokers.put(brokerAddr, null);
			connectToNewBroker(brokerAddr, sess);
		} 
	}
	
	protected void onSessionDestroyed(Session sess) throws IOException {
		subscribers.remove(sess); 
	}
	
	@Override
	protected void onException(Throwable e, Session sess) throws IOException {
		subscribers.remove(sess);
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
