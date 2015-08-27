package org.zbus.broker.ha;

import java.io.IOException;
import java.util.Iterator;
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

import com.alibaba.fastjson.JSON;

public class TrackServer extends MessageAdaptor{
	static final Logger log = Logger.getLogger(TrackServer.class); 
	HaBrokerEntrySet brokerEntrySet = new HaBrokerEntrySet(); 
	
	Map<String, Session> subscribers =  new ConcurrentHashMap<String, Session>();
	Map<String, Session> brokers = new ConcurrentHashMap<String, Session>();
	
	public TrackServer(){  
		cmd(Protocol.EntryUpdate, entryUpdateHandler); 
		cmd(Protocol.EntryQueryAll, entryQueryAllHandler);
		cmd(Protocol.EntrySubscribe, entrySubscribeHandler);
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
				brokers.remove(sess.id());
				String broker = sess.attr("broker");
				if(broker == null) return;
				brokerEntrySet.removeBroker(broker);
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
	
	private MessageHandler entryUpdateHandler = new MessageHandler() { 
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
	}; 
	
	private MessageHandler entryQueryAllHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			Message res = new Message();
			res.setId(msg.getId());
			res.setBody(brokerEntryTableJson());
			sess.write(res);
		}
	};
	
	private MessageHandler entrySubscribeHandler = new MessageHandler() {
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			subscribers.put(sess.id(), sess);
		}
	};
	
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
