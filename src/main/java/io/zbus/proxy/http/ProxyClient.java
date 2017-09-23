package io.zbus.proxy.http;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.kit.ThreadKit.ManualResetEvent;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.mq.Message;
import io.zbus.mq.MqClient;
import io.zbus.transport.CodecInitializer;
import io.zbus.transport.EventLoop;
import io.zbus.transport.Session;
import io.zbus.transport.http.MessageCodec;
import io.zbus.transport.tcp.TcpClient; 

public class ProxyClient extends TcpClient<io.zbus.transport.http.Message, io.zbus.transport.http.Message> { 
	private static final Logger log = LoggerFactory.getLogger(ProxyClient.class); 
	private Map<String, Context> requestTable = new ConcurrentHashMap<String, Context>();  
	private boolean targetMessageIdentifiable = true;
	private MessageFilter recvFilter; 
	private int defaultTimeout = 10000;
	
	private Context lastestContext;
	private ManualResetEvent latestReturned = new ManualResetEvent(true);
	
	private static class Context{
		String topic;
		String msgId;
		String sender;
		MqClient senderClient;
	}
	
	public ProxyClient(String address, final EventLoop loop, int heartbeatInterval) {  
		super(address, loop);     
		
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
				p.add(new MessageCodec());
			}
		});  
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		Message res = new Message((io.zbus.transport.http.Message)msg); 
		
		String msgId = res.getId(); 
		if(msgId == null){
			targetMessageIdentifiable = false;
		}
		Context ctx = null;
		if(targetMessageIdentifiable){ 
			ctx = requestTable.remove(msgId); 
		} else {
			synchronized (latestReturned) {
				ctx = lastestContext;
				lastestContext = null; //clear
			} 
		}  
		
		if(ctx == null){  
			return; //ignore
		}
		
		if(recvFilter != null){
			if( recvFilter.filter(res, ctx.senderClient) == false){
				return;
			}
		} 
		
		res.setId(ctx.msgId);
		res.setTopic(ctx.topic);
		res.setReceiver(ctx.sender); 
		try {
			ctx.senderClient.route(res);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} finally{
			if(!targetMessageIdentifiable){
				latestReturned.set();
			}
		} 
	} 
	
	public void sendMessage(Message msg, MqClient senderClient, int timeoutMs) 
			throws IOException, InterruptedException{  
		if(!targetMessageIdentifiable){ 
			synchronized (latestReturned) {
				latestReturned.await(timeoutMs, TimeUnit.MILLISECONDS);
				if(!latestReturned.isSignalled()){
					throw new IOException("waiting result timeout, should close connection");
				}  
			} 
		}
		
		Context ctx = new Context();
		ctx.msgId = msg.getId();
		ctx.topic = msg.getTopic();
		ctx.sender = msg.getSender();
		ctx.senderClient = senderClient;
		
		synchronized (latestReturned) {
			lastestContext = ctx; 
			latestReturned.reset();
		} 
		
		if(targetMessageIdentifiable){
			requestTable.put(ctx.msgId, ctx); 
		}  
		
		super.sendMessage(msg);   
	}
	
	public void sendMessage(Message msg, MqClient senderClient) 
			throws IOException, InterruptedException{
		sendMessage(msg, senderClient, defaultTimeout); //default to 
	}  

	public MessageFilter getRecvFilter() {
		return recvFilter;
	}

	public void setRecvFilter(MessageFilter recvFilter) {
		this.recvFilter = recvFilter;
	}

	public int getDefaultTimeout() {
		return defaultTimeout;
	}

	public void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	} 
	
}  
