package org.zbus.server;
 

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.common.BrokerInfo;
import org.zbus.common.BrokerMqInfo;
import org.zbus.common.MessageMode;
import org.zbus.common.Proto;
import org.zbus.logging.Logger;
import org.zbus.logging.LoggerFactory;
import org.zbus.remoting.Helper;
import org.zbus.remoting.Message;
import org.zbus.remoting.MessageHandler;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.RemotingServer;
import org.zbus.remoting.callback.ErrorCallback;
import org.zbus.remoting.znet.Session;
import org.zbus.server.mq.AbstractMQ;
import org.zbus.server.mq.MqStore;
import org.zbus.server.mq.PubSub;
import org.zbus.server.mq.ReplyHelper;
import org.zbus.server.mq.MQ;
 
public class ZbusServer extends RemotingServer {
	private static final Logger log = LoggerFactory.getLogger(ZbusServer.class);
	
	private String adminToken = ""; 
	private long trackDelay = 1000;
	private long trackInterval = 3000;
	private long mqCleanDelay = 1000;
	private long mqCleanInterval = 3000;
	private long mqPersistDelay = 3000;
	private long mqPersistInterval = 10000;
	private boolean loadMqFromDump = true;
	 
	private AdminHandler adminHandler;
	private final Timer trackReportTimer = new Timer("TrackReportTimer", true); 
	private final Timer mqSessionCleanTimer = new Timer("MqSessionCleanTimer", true); 
	private final Timer mqPersistTimer = new Timer("MqPersistTimer", true);
	private final List<RemotingClient> trackClients = new ArrayList<RemotingClient>();
	
	
	private ExecutorService reportExecutor = new ThreadPoolExecutor(4, 
			16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	
	private ExecutorService mqExecutor = new ThreadPoolExecutor(4, 
			16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	
	private final ConcurrentMap<String, AbstractMQ> mqTable = new ConcurrentHashMap<String, AbstractMQ>();  
	
	private final MqStore mqStore = new MqStore(this.mqTable);
	
	
	public ZbusServer(int serverPort) throws IOException {
		this("0.0.0.0", serverPort);
	}
	
	public ZbusServer(String serverHost, int serverPort) throws IOException {
		super(serverHost, serverPort, new ZbusServerDispachterManager()); 
		this.ownDispachterManager = true; 
		
		ZbusServerEventAdaptor eventHandler = (ZbusServerEventAdaptor) this.serverHandler;
		eventHandler.setMqTable(mqTable);
		
		this.serverName = "ZbusServer";
    	this.adminHandler = new AdminHandler();
    	this.adminHandler.setAdminToken(this.adminToken);
    	
    	if(loadMqFromDump){
    		this.loadMqTableFromDump();
    	}
    	
    	this.mqSessionCleanTimer.scheduleAtFixedRate(new TimerTask() { 
			@Override
			public void run() {  
				Iterator<Entry<String, AbstractMQ>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, AbstractMQ> e = iter.next();
		    		AbstractMQ mq = e.getValue(); 
		    		mq.cleanSession();
		    	}
				
			}
		}, mqCleanDelay, mqCleanInterval);
    	
    	this.mqPersistTimer.scheduleAtFixedRate(new TimerTask() { 
			@Override
			public void run() {   
				mqStore.dump();
			}
		}, mqPersistDelay, mqPersistInterval);
	}  
	 
	private void loadMqTableFromDump(){
		ConcurrentMap<String, AbstractMQ> load = MqStore.load();
		if(load == null) return;
		Iterator<Entry<String, AbstractMQ>> iter = load.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, AbstractMQ> e = iter.next();
			AbstractMQ mq = e.getValue();
			mq.restoreFromDump(mqExecutor); 
			this.mqTable.put(e.getKey(), mq);
		}
	}
	private AbstractMQ findMQ(Message msg, Session sess) throws IOException{
		String mqName = msg.getMq();
		AbstractMQ mq = mqTable.get(mqName);
    	boolean ack = msg.isAck();
    	if(mq == null){
    		if(ack){
    			ReplyHelper.reply404(msg, sess);
    		}
    		return null;
    	} 
    	
    	if(!"".equals(mq.getAccessToken())){ 
    		if(!mq.getAccessToken().equals(msg.getToken())){ 
    			if(ack){
    				ReplyHelper.reply403(msg, sess);
    			}
    			return null;
    		}
    	} 
    	
    	return mq;
    	
	}
	
	public void init(){  
		
		this.registerGlobalHandler(new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				String mqReply = msg.getMqReply();
				if(mqReply == null ||  mqReply.equals("")){
					msg.setMqReply(sess.id()); //reply default to self
				}   
				msg.setHead(Message.HEADER_CLIENT, sess.getRemoteAddress());
				msg.setHead(Message.HEADER_BROKER, serverAddr); 
				
				if(!Message.HEARTBEAT.equals(msg.getCommand())){
					log.info("%s", msg);
				}
			}
		}); 


		this.registerHandler(Proto.Heartbeat, new MessageHandler() {
			
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				//ignore;
			}
		});

		this.registerHandler(Proto.Produce, new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException { 
				AbstractMQ mq = findMQ(msg, sess);
				if(mq == null) return;
				mq.produce(msg, sess); 
			}
		});
		
		this.registerHandler(Proto.Consume, new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException { 
				AbstractMQ mq = findMQ(msg, sess);
				if(mq == null) return;
				mq.consume(msg, sess); 
			}
		});
		
		
		this.registerHandler(Proto.Request, new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException { 
				AbstractMQ reqMq = findMQ(msg, sess);
				if(reqMq == null) return;
				
				
				String replyMqName = msg.getMqReply();  
				AbstractMQ replyMq = mqTable.get(replyMqName);
				if(replyMq == null){
					int mode = MessageMode.intValue(MessageMode.MQ, MessageMode.Temp);
					replyMq = new MQ(replyMqName, mqExecutor, mode); 
					replyMq.setCreator(sess.getRemoteAddress());
					mqTable.putIfAbsent(replyMqName, replyMq);
				} 
				msg.setAck(false);
				
				Message msgConsume = Message.copyWithoutBody(msg);
				reqMq.produce(msg, sess); 
				replyMq.consume(msgConsume, sess);
			}
		});
		
		
		//初始化管理处理回调
		this.registerHandler(Proto.Admin, adminHandler);
		
		adminHandler.registerHandler(Proto.CreateMQ, new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				String msgId= msg.getMsgId();
				String mqName = msg.getHeadOrParam("mq_name");
	    		String accessToken = msg.getHeadOrParam("access_token", "");
	    		String type = msg.getHeadOrParam("mq_mode", "");
	    		int mode = 0;
	    		try{
	    			mode = Integer.valueOf(type);
	    		} catch (Exception e){
	    			msg.setBody("mq_mode invalid");
	    			ReplyHelper.reply400(msg, sess);
	        		return;  
	    		}
	    		
	    		
	    		if(mqName == null){
	    			msg.setBody("Missing mq_name filed");
	    			ReplyHelper.reply400(msg, sess);
	        		return;  
	    		} 
	    		
	    		AbstractMQ mq = mqTable.get(mqName);
	    		if(mq == null){
	    			if(MessageMode.isEnabled(mode, MessageMode.PubSub)){
	    				mq = new PubSub(mqName, mqExecutor, mode);
	    			} else {//默认到消息队列
	    				mq = new MQ(mqName, mqExecutor, mode);
	    			} 
	    			mq.setAccessToken(accessToken);
		    		mq.setCreator(sess.getRemoteAddress());
		    		mqTable.putIfAbsent(mqName, mq);
					log.info("MQ Created: %s", mq);
					ReplyHelper.reply200(msgId, sess); 
					
		    		reportToTrackServer();
		    		return;
	    		}
	    		
	    		if(MessageMode.isEnabled(mode, MessageMode.MQ) && !(mq instanceof MQ)){
    				msg.setBody("MsgQueue, type not matched");
	    			ReplyHelper.reply400(msg, sess);
	        		return;  
    			}
	    		if(MessageMode.isEnabled(mode, MessageMode.PubSub) && !(mq instanceof PubSub)){
    				msg.setBody("Pubsub, type not matched");
	    			ReplyHelper.reply400(msg, sess);
	        		return;  
    			}
    			ReplyHelper.reply200(msgId, sess);  
			}
		}); 
		//WEB监控
		intiMonitor(); 
	} 
	 
	
	private void intiMonitor() { 
		this.registerHandler("", new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				msg = new Message();
				msg.setStatus("200");
				msg.setHead("content-type","text/html");
				String body = Helper.loadFileContent("zbus.htm"); 
				msg.setBody(body); 
				sess.write(msg);  
			}
		});
		
		this.registerHandler("jquery", new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				msg = new Message();
				msg.setStatus("200");
				msg.setHead("content-type","application/javascript");
				String body = Helper.loadFileContent("jquery.js"); 
				msg.setBody(body); 
				sess.write(msg);  
			}
		});
		
		this.registerHandler("data", new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException {
				msg = packServerInfo();
				msg.setStatus("200"); 
				msg.setHead("content-type", "application/json");
				sess.write(msg);  
			}
		});
	
	}
	
	public Map<String, BrokerMqInfo> getMqInfoTable(){
   		Map<String, BrokerMqInfo> table = new HashMap<String, BrokerMqInfo>();
   		for(Map.Entry<String, AbstractMQ> e : this.mqTable.entrySet()){
   			table.put(e.getKey(), e.getValue().getMqInfo());
   		}
   		return table;
   	}
	
	private Message packServerInfo(){
		Message msg = new Message(); 
		BrokerInfo info = new BrokerInfo();
		info.setBroker(serverAddr);
		info.setMqTable(getMqInfoTable());   
		msg.setBody(info.toString());
		return msg;
	}
	
	
	
	public void setupTrackServer(String trackServerAddr){ 
		if(trackServerAddr == null) return;
		trackClients.clear();
		String[] serverAddrs = trackServerAddr.split("[;]");
		for(String addr : serverAddrs){
			addr = addr.trim();
			if( addr.isEmpty() ) continue;
			RemotingClient client = new RemotingClient(addr);
			client.onError(new ErrorCallback() { 
				@Override
				public void onError(IOException e, Session sess) throws IOException {
					//ignore
				}
			});
			trackClients.add(client);
		} 
		
		this.trackReportTimer.scheduleAtFixedRate(new TimerTask() { 
			@Override
			public void run() { 
				reportToTrackServer();
			}
		}, trackDelay, trackInterval);
	}
	
	private void reportToTrackServer(){
		reportExecutor.submit(new Runnable() { 
			@Override
			public void run() {
				Message msg = packServerInfo();
				msg.setCommand(Proto.TrackReport);
				for(RemotingClient client : trackClients){
					try {
						client.invokeAsync(msg, null);
					} catch (IOException e) {  
						//ignore
					}
				} 
			}
		}); 
	}

	public void setAdminToken(String adminToken) {
		this.adminToken = adminToken;
	}
	public void setTrackDelay(long trackDelay) {
		this.trackDelay = trackDelay;
	}
	public void setTrackInterval(long trackInterval) {
		this.trackInterval = trackInterval;
	}

	public void close(){
		this.trackReportTimer.cancel();
		this.mqSessionCleanTimer.cancel();
		for(RemotingClient client : this.trackClients){
			client.close();
		}  
		super.close();
	}
	
	
	public static void main(String[] args) throws Exception{
		int serverPort = Helper.option(args, "-p", 15555);
		String adminToken = Helper.option(args, "-adm", "");
		String trackServerAddr = Helper.option(args, "-track", 
				"127.0.0.1:16666;127.0.0.1:16667");


		ZbusServer zbus = new ZbusServer(serverPort); 
		zbus.setAdminToken(adminToken);
		zbus.setupTrackServer(trackServerAddr); 
		zbus.start();  
	} 
	 
}
