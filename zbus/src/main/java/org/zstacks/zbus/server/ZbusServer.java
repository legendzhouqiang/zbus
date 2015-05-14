package org.zstacks.zbus.server;
 

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.protocol.MessageMode;
import org.zstacks.zbus.protocol.Proto;
import org.zstacks.zbus.server.mq.MessageQueue;
import org.zstacks.zbus.server.mq.ReplyQueue;
import org.zstacks.zbus.server.mq.store.MessageStore;
import org.zstacks.zbus.server.mq.store.MessageStoreFactory;
import org.zstacks.znet.Helper;
import org.zstacks.znet.Message;
import org.zstacks.znet.MessageHandler;
import org.zstacks.znet.RemotingServer;
import org.zstacks.znet.nio.Dispatcher;
import org.zstacks.znet.nio.Session;


public class ZbusServer extends RemotingServer {
	private static final Logger log = LoggerFactory.getLogger(ZbusServer.class);
	
	private final ExecutorService executorService;
	private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
	private long mqCleanInterval = 3000;   
	
	private final ConcurrentMap<String, MessageQueue> mqTable = new ConcurrentHashMap<String, MessageQueue>();  
 
	private MessageStore messageStore;
	private String messageStoreType = "dummy"; 
	
	private String adminToken = "";  
	
	private final AdminHandler adminHandler;
	
	private final TrackReport trackReport; 
	
	
	public ZbusServer(int serverPort, Dispatcher dispatcher) throws IOException {
		this("0.0.0.0", serverPort, dispatcher);
	}
	
	public ZbusServer(String serverHost, int serverPort, Dispatcher dispatcher) throws IOException {
		super(serverHost, serverPort, dispatcher); 
		if(!dispatcher.isStarted()){
			dispatcher.start();
		}
		
		this.executorService = dispatcher.executorService();
		this.serverName = "ZbusServer";
		this.trackReport = new TrackReport(mqTable, serverAddr);//TrackReport尚未启动自动上报
		
    	this.adminHandler = new AdminHandler(mqTable, executorService, serverAddr, trackReport);
    	this.adminHandler.setAccessToken(this.adminToken); 
    	
    	this.initHandlers();
	}  
	 
	
	private MessageQueue findMQ(Message msg, Session sess) throws IOException{
		String mqName = msg.getMq();
		if(mqName == null){
			mqName = msg.getPath(); //support browser
		}
		MessageQueue mq = mqTable.get(mqName);
    	boolean ack = msg.isAck();
    	if(mq == null){
    		if(ack){
    			ServerHelper.reply404(msg, sess);
    		}
    		return null;
    	} 
    	
    	if(!"".equals(mq.getAccessToken())){ 
    		if(!mq.getAccessToken().equals(msg.getToken())){ 
    			if(ack){
    				ServerHelper.reply403(msg, sess);
    			}
    			return null;
    		}
    	} 
    	
    	return mq;
    	
	}
	
	private void initHandlers(){  
		
		this.registerGlobalHandler(new MessageHandler() { 
			public void handleMessage(Message msg, Session sess) throws IOException {
				String mqReply = msg.getMqReply();
				if(mqReply == null ||  mqReply.equals("")){
					msg.setMqReply(sess.id()); 
				}   
				if(msg.getMsgId() == null){
					msg.setMsgId(UUID.randomUUID().toString());
				}
				msg.setHead(Message.HEADER_REMOTE_ADDR, sess.getRemoteAddress());
				msg.setHead(Message.HEADER_BROKER, serverAddr);  
				if(!Message.HEARTBEAT.equals(msg.getCommand())){
					log.debug("{}", msg);
				}
			}
		}); 

		this.registerHandler(Proto.Produce, new MessageHandler() { 
			public void handleMessage(Message msg, Session sess) throws IOException { 
				MessageQueue mq = findMQ(msg, sess);
				if(mq == null) return;
				mq.produce(msg, sess); 
			}
		});
		
		this.registerHandler(Proto.Consume, new MessageHandler() { 
			public void handleMessage(Message msg, Session sess) throws IOException { 
				MessageQueue mq = findMQ(msg, sess);
				if(mq == null) return;
				mq.consume(msg, sess); 
			}
		});
		
		
		this.registerHandler(Proto.Request, new MessageHandler() { 
			public void handleMessage(Message requestMsg, Session sess) throws IOException { 
				MessageQueue requestMq = findMQ(requestMsg, sess);
				if(requestMq == null) return;
				
				
				String replyMqName = requestMsg.getMqReply();  
				MessageQueue replyMq = mqTable.get(replyMqName);
				if(replyMq == null){
					int mode = MessageMode.intValue(MessageMode.MQ, MessageMode.Temp);
					replyMq = new ReplyQueue(serverAddr, replyMqName, executorService, mode); 
					replyMq.setCreator(sess.getRemoteAddress());
					mqTable.putIfAbsent(replyMqName, replyMq);
				} 
				requestMsg.setAck(false);
				
				Message msgConsume = Message.copyWithoutBody(requestMsg);
				requestMq.produce(requestMsg, sess); 
				replyMq.consume(msgConsume, sess);
			}
		});
		
		this.registerHandler(Proto.Admin, adminHandler); 
	} 
	 
	public void setAdminToken(String adminToken) {
		this.adminToken = adminToken;
	} 
	
	@Override
	public void start() throws IOException { 
		super.start();
		//build message store
		this.messageStore = MessageStoreFactory.getMessageStore(this.serverAddr, this.messageStoreType);
		this.adminHandler.setMessageStore(this.messageStore); 
		{
			log.info("message store loading ....");
			this.mqTable.clear();
			try{
				ConcurrentMap<String, MessageQueue> mqs = this.messageStore.loadMqTable();
				Iterator<Entry<String, MessageQueue>> iter = mqs.entrySet().iterator();
				while(iter.hasNext()){
					MessageQueue mq = iter.next().getValue();
					mq.setExecutor(this.executorService);
				} 
				this.mqTable.putAll(mqs);
				log.info("message store loaded");
			} catch(Exception e){
				log.info("message store loading error: {}", e.getMessage(), e);
			} 
		} 
	
		this.scheduledExecutor.scheduleAtFixedRate(new Runnable() { 
			public void run() {  
				Iterator<Entry<String, MessageQueue>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, MessageQueue> e = iter.next();
		    		MessageQueue mq = e.getValue(); 
		    		mq.cleanSession();
		    	}
			}
		}, 1000, mqCleanInterval, TimeUnit.MILLISECONDS);
	}
	
	public void close() throws IOException{  
		this.scheduledExecutor.shutdown(); 
		this.trackReport.close(); 
		
		super.close();
	}
	
	public void startTrackReport(String trackServerAddr){
		try {
			this.trackReport.startTrackReport(trackServerAddr, dispatcher);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	public void setMessageStoreType(String messageStoreType) {
		this.messageStoreType = messageStoreType;
	}
	
	 
    @Override
    public void onException(Throwable e, Session sess) throws IOException {
    	if(! (e instanceof IOException) ){
			super.onException(e, sess);
		}
    	this.cleanMQ(sess);
    }
    
    @Override
    public void onSessionDestroyed(Session sess) throws IOException {  
    	this.cleanMQ(sess);
    }
    
    
    @Override
    public String findHandlerKey(Message msg) {
    	//1 优先使用消息中的cmd字段做为命令控制字段
    	String cmd = msg.getCommand();
    	if(cmd == null){ 
    		//2 如果cmd未设置，选择Message中path作为命令控制
    		cmd = msg.getPath(); 
    	}
    	//3 都没设置，默认为管理控制命令
    	if(cmd == null || "".equals(cmd.trim())){  
    		cmd = Proto.Admin; 
    	}
    	return cmd;
    }
    
    private void cleanMQ(Session sess){
    	if(this.mqTable == null) return;
    	String creator = sess.getRemoteAddress();
    	Iterator<Entry<String, MessageQueue>> iter = this.mqTable.entrySet().iterator();
    	while(iter.hasNext()){
    		Entry<String, MessageQueue> e = iter.next();
    		MessageQueue mq = e.getValue();
    		if(MessageMode.isEnabled(mq.getMode(), MessageMode.Temp)){
    			if(mq.getCreator().equals(creator)){
        			iter.remove();
        		}
    		} 
    	}
    } 
  
    public String getServerAddress(){
    	return this.serverAddr;
    }
    
    
    public static class ZbusServerConfig{
    	public int serverPort = 15555;
    	public String adminToken = "";
    	public String trackServerAddr; //用分号分割, 127.0.0.1:16666;127.0.0.1:16667
    	public String storeType = "dummy";  
    	public int selectorCount = 1;
    	public int executorCount = 4;
    } 

    @SuppressWarnings("resource")
    public static void main(String[] args) throws Exception{
    	ZbusServerConfig config = new ZbusServerConfig();
    	config.serverPort = Helper.option(args, "-p", 15555); 
    	config.adminToken = Helper.option(args, "-admin", "");
    	config.trackServerAddr = Helper.option(args, "-track", "127.0.0.1:16666;127.0.0.1:16667");
    	config.storeType = Helper.option(args, "-store", "dummy");   
    	config.selectorCount = Helper.option(args, "-selector", 1);   
    	config.executorCount = Helper.option(args, "-executor", 16); 
    	 
		Dispatcher dispatcher = new Dispatcher() 
    		.selectorCount(config.selectorCount)
    		.executorCount(config.executorCount); 
    	
		ZbusServer zbus = new ZbusServer(config.serverPort, dispatcher);  
		zbus.setAdminToken(config.adminToken);
		zbus.setMessageStoreType(config.storeType);
		
		//HA高可用模式下，启动链接TrackServer，上报当前节点拓扑信息
		if(config.trackServerAddr != null && !config.trackServerAddr.equals("")){
			zbus.startTrackReport(config.trackServerAddr); 
		}
		
		zbus.start();
	}  
}

