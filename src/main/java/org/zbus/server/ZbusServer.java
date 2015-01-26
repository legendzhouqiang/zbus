package org.zbus.server;
 

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.zbus.common.BrokerInfo;
import org.zbus.common.MqInfo;
import org.zbus.common.Helper;
import org.zbus.common.MessageMode;
import org.zbus.common.Proto;
import org.zbus.common.json.JSON;
import org.zbus.common.logging.Logger;
import org.zbus.common.logging.LoggerFactory;
import org.zbus.remoting.Message;
import org.zbus.remoting.MessageHandler;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.RemotingServer;
import org.zbus.remoting.ServerDispatcherManager;
import org.zbus.remoting.ServerEventAdaptor;
import org.zbus.remoting.callback.ErrorCallback;
import org.zbus.remoting.nio.DispatcherManager;
import org.zbus.remoting.nio.Session;
import org.zbus.server.mq.MessageQueue;
import org.zbus.server.mq.ReplyQueue;
import org.zbus.server.mq.RequestQueue;
import org.zbus.server.mq.MqStore;
import org.zbus.server.mq.PubsubQueue;
import org.zbus.server.mq.ReplyHelper;
 
public class ZbusServer extends RemotingServer {
	private static final Logger log = LoggerFactory.getLogger(ZbusServer.class);
	
	private String adminToken = ""; 
	private long trackDelay = 1000;
	private long trackInterval = 3000;
	
	private long mqCleanDelay = 1000;
	private long mqCleanInterval = 3000;
	private long mqPersistDelay = 3000;
	private long mqPersistInterval = 3000;
	private boolean loadMqFromDump = false;
	private boolean persistEnabled = false;
	 
	private AdminHandler adminHandler;

	protected final ScheduledExecutorService trackReportService = Executors.newSingleThreadScheduledExecutor();
	protected final ScheduledExecutorService mqSessionCleanService = Executors.newSingleThreadScheduledExecutor();
	protected final ScheduledExecutorService mqPersistService = Executors.newSingleThreadScheduledExecutor();
	
	private final List<RemotingClient> trackClients = new ArrayList<RemotingClient>();
	
	
	private ExecutorService reportExecutor = new ThreadPoolExecutor(4,16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	private ExecutorService mqExecutor = new ThreadPoolExecutor(4, 16, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	
	private final ConcurrentMap<String, MessageQueue> mqTable = new ConcurrentHashMap<String, MessageQueue>();  
	private final MqStore mqStore = new MqStore(this.mqTable);
	
	
	public ZbusServer(int serverPort) throws IOException {
		this("0.0.0.0", serverPort);
	}
	
	public ZbusServer(String serverHost, int serverPort) throws IOException {
		super(serverHost, serverPort, new ZbusServerDispachterManager());  
		
		ZbusServerEventAdaptor eventHandler = (ZbusServerEventAdaptor) this.serverHandler;
		eventHandler.setMqTable(mqTable); 
		this.serverName = "ZbusServer";
    	this.adminHandler = new AdminHandler();
    	this.adminHandler.setAdminToken(this.adminToken);
    	
    	if(loadMqFromDump){
    		this.loadMqTableFromDump();
    	} 
	}  
	 
	private void loadMqTableFromDump(){
		ConcurrentMap<String, MessageQueue> load = MqStore.load();
		if(load == null) return;
		Iterator<Entry<String, MessageQueue>> iter = load.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, MessageQueue> e = iter.next();
			MessageQueue mq = e.getValue();
			mq.restoreFromDump(mqExecutor); 
			this.mqTable.put(e.getKey(), mq);
		}
	}
	
	
	private MessageQueue findMQ(Message msg, Session sess) throws IOException{
		String mqName = msg.getMq();
		MessageQueue mq = mqTable.get(mqName);
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
				msg.setHead(Message.HEADER_REMOTE_ADDR, sess.getRemoteAddress());
				msg.setHead(Message.HEADER_BROKER, serverAddr); 
				msg.setHead(Message.HEADER_REMOTE_ID, sess.id());
				
				if(!Message.HEARTBEAT.equals(msg.getCommand())){
					log.debug("%s", msg);
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
				MessageQueue mq = findMQ(msg, sess);
				if(mq == null) return;
				mq.produce(msg, sess); 
			}
		});
		
		this.registerHandler(Proto.Consume, new MessageHandler() { 
			@Override
			public void handleMessage(Message msg, Session sess) throws IOException { 
				MessageQueue mq = findMQ(msg, sess);
				if(mq == null) return;
				mq.consume(msg, sess); 
			}
		});
		
		
		this.registerHandler(Proto.Request, new MessageHandler() { 
			@Override
			public void handleMessage(Message requestMsg, Session sess) throws IOException { 
				MessageQueue requestMq = findMQ(requestMsg, sess);
				if(requestMq == null) return;
				
				
				String replyMqName = requestMsg.getMqReply();  
				MessageQueue replyMq = mqTable.get(replyMqName);
				if(replyMq == null){
					int mode = MessageMode.intValue(MessageMode.MQ, MessageMode.Temp);
					replyMq = new ReplyQueue(serverAddr, replyMqName, mqExecutor, mode); 
					replyMq.setCreator(sess.getRemoteAddress());
					mqTable.putIfAbsent(replyMqName, replyMq);
				} 
				requestMsg.setAck(false);
				
				Message msgConsume = Message.copyWithoutBody(requestMsg);
				requestMq.produce(requestMsg, sess); 
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
	    		
	    		MessageQueue mq = mqTable.get(mqName);
	    		if(mq == null){
	    			if(MessageMode.isEnabled(mode, MessageMode.PubSub)){
	    				mq = new PubsubQueue(serverAddr, mqName, mqExecutor, mode);
	    			} else {//默认到消息队列
	    				mq = new RequestQueue(serverAddr, mqName, mqExecutor, mode);
	    			} 
	    			mq.setAccessToken(accessToken);
		    		mq.setCreator(sess.getRemoteAddress());
		    		mqTable.putIfAbsent(mqName, mq);
					log.info("MQ Created: %s", mq);
					ReplyHelper.reply200(msgId, sess); 
					
		    		reportToTrackServer();
		    		return;
	    		}
	    		
	    		if(MessageMode.isEnabled(mode, MessageMode.MQ) && !(mq instanceof RequestQueue)){
    				msg.setBody("MsgQueue, type not matched");
	    			ReplyHelper.reply400(msg, sess);
	        		return;  
    			}
	    		if(MessageMode.isEnabled(mode, MessageMode.PubSub) && !(mq instanceof PubsubQueue)){
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
	
	
	private Message packServerInfo(){
		Map<String, MqInfo> table = new HashMap<String, MqInfo>();
   		for(Map.Entry<String, MessageQueue> e : this.mqTable.entrySet()){
   			table.put(e.getKey(), e.getValue().getMqInfo());
   		} 
		Message msg = new Message(); 
		BrokerInfo info = new BrokerInfo();
		info.setBroker(serverAddr);
		info.setMqTable(table);  
		msg.setBody(JSON.toJSONString(info));
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
		
		this.trackReportService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() { 
				reportToTrackServer();
			}
		}, trackDelay, trackInterval, TimeUnit.MILLISECONDS);
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

	@Override
	public void start() throws Exception { 
		super.start();
		
		this.mqSessionCleanService.scheduleAtFixedRate(new Runnable() { 
			@Override
			public void run() {  
				Iterator<Entry<String, MessageQueue>> iter = mqTable.entrySet().iterator();
		    	while(iter.hasNext()){
		    		Entry<String, MessageQueue> e = iter.next();
		    		MessageQueue mq = e.getValue(); 
		    		mq.cleanSession();
		    	}
				
			}
		}, mqCleanDelay, mqCleanInterval, TimeUnit.MILLISECONDS);
		
    	if(this.persistEnabled){
	    	this.mqPersistService.scheduleAtFixedRate(new Runnable() { 
				@Override
				public void run() {   
					mqStore.dump();
				}
			}, mqPersistDelay, mqPersistInterval, TimeUnit.MILLISECONDS);
    	}
	}
	
	public void close(){
		this.trackReportService.shutdown();
		this.mqSessionCleanService.shutdown();
		this.mqPersistService.shutdown();
		for(RemotingClient client : this.trackClients){
			client.close();
		}   
	}
	
	public void setMqPersistInterval(long mqPersistInterval) {
		this.mqPersistInterval = mqPersistInterval;
	}
	

	public void setPersistEnabled(boolean persistEnabled) {
		this.persistEnabled = persistEnabled;
	}

	public static void main(String[] args) throws Exception{
		int serverPort = Helper.option(args, "-p", 15555);
		boolean persistEnabled = Helper.option(args, "-bw", false);
		int persistInterval = Helper.option(args, "-w", 3000);
		String adminToken = Helper.option(args, "-adm", "");
		String trackServerAddr = Helper.option(args, "-track", 
				"127.0.0.1:16666;127.0.0.1:16667");


		ZbusServer zbus = new ZbusServer(serverPort); 
		zbus.setPersistEnabled(persistEnabled);
		zbus.setAdminToken(adminToken);
		zbus.setupTrackServer(trackServerAddr); 
		zbus.setMqPersistInterval(persistInterval);
		
		zbus.start();  
	}  
}
 

class ZbusServerEventAdaptor extends ServerEventAdaptor{ 
	private ConcurrentMap<String, MessageQueue> mqTable = null;
	 
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
    
	public void setMqTable(ConcurrentMap<String, MessageQueue> mqTable) {
		this.mqTable = mqTable;
	}  
}


class ZbusServerDispachterManager extends ServerDispatcherManager{ 
	private ServerEventAdaptor serverEventAdaptor = new ZbusServerEventAdaptor();
	
	public ZbusServerDispachterManager(  
			ExecutorService executor, 
			int engineCount, 
			String engineNamePrefix) throws IOException{
		
		super(executor,engineCount, ZbusServerDispachterManager.class.getSimpleName());
	}
	
	public ZbusServerDispachterManager(int engineCount) throws IOException {
		this(DispatcherManager.newDefaultExecutor(), 
				engineCount, ZbusServerDispachterManager.class.getSimpleName());
	}

	public ZbusServerDispachterManager() throws IOException  { 
		this(DispatcherManager.defaultDispatcherSize());
	}  
	
	@Override
	public ServerEventAdaptor buildEventAdaptor() {  
		//服务器端，所有Session共享一个事件处理器
		return this.serverEventAdaptor;
	}
}

