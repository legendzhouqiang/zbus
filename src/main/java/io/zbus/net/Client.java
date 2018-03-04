package io.zbus.net;
 
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.zbus.kit.NetKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory; 


public class Client<REQ, RES> implements Closeable{
	private static final Logger log = LoggerFactory.getLogger(Client.class); 
	  
	public volatile MessageHandler<RES> onMessage; 
	public volatile ErrorHandler onError;
	public volatile EventHandler onOpen;
	public volatile EventHandler onClose;   
	
	public long connectTimeout = 3000;  
	public long reconnectDelay = 3000;  
	
	
	protected final String host;
	protected final int port;   
	
	protected Bootstrap bootstrap;
	protected final EventLoopGroup group;  
	protected SslContext sslCtx; 
	protected ChannelFuture channelFuture; 
	protected CodecInitializer codecInitializer;   
	
	protected volatile ScheduledExecutorService heartbeator = null; 
	protected MessageBuilder<REQ> heartbeatMessageBuilder;   
	
	protected Session session;  
	protected IoAdaptor ioAdaptor;
	
	protected CountDownLatch activeLatch = new CountDownLatch(1);     
	protected List<REQ> sendingMessages = Collections.synchronizedList(new ArrayList<>());
	
	private ConnectionStatus status = ConnectionStatus.New;
	
	public Client(String address, EventLoop loop){   
		group = loop.getGroup(); 
		
		Object[] hp = NetKit.hostPort(address);
		this.host = (String)hp[0];
		this.port = (Integer)hp[1]; 
		
		onOpen = ()->{
			String msg = String.format("Connection(%s) OK", serverAddress());
			log.info(msg); 
		};
		
		onClose = ()->{
			log.warn("Disconnected from(%s)", serverAddress());
			try {
				Thread.sleep(reconnectDelay);
			} catch (InterruptedException e1) {
				return;
			}
			
			connect();
		};  
		
		ioAdaptor = new IoAdaptor() {  
			@Override
			public void sessionCreated(Session session) throws IOException { 
				Client.this.session = session; 
				activeLatch.countDown();
				status = ConnectionStatus.Open;
				if(onOpen != null){
					onOpen.handle();
				}
			}

			public void sessionToDestroy(Session session) throws IOException {
				if(Client.this.session != null){
					Client.this.session.close(); 
					Client.this.session = null;
				} 
				status = ConnectionStatus.Closed;
				if(onClose != null){
					onClose.handle();
				}   
			} 

			@Override
			public void onError(Throwable e, Session sess) throws IOException { 
				if(onError != null){
					onError.handle(e);
				} else {
					log.error(e.getMessage(), e);
				}
			} 
			
			@Override
			public void onIdle(Session sess) throws IOException { 
				
			}
			  
			
			@Override
			public void onMessage(Object msg, Session sess) throws IOException {
				@SuppressWarnings("unchecked")
				RES res = (RES)msg;     
		    	if(onMessage != null){
		    		onMessage.handle(res);
		    		return;
		    	}  
		    	log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", res);
			}    
		};
	}  
	
	  
	protected String serverAddress(){
		return String.format("%s%s:%d", sslCtx==null? "" : "[SSL]", host, port);
	}
	
	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	}  

	public synchronized void connect(){  
		init(); 
		activeLatch = new CountDownLatch(1);
		status = ConnectionStatus.Connecting;
		channelFuture = bootstrap.connect(host, port);
	}    
	
	private void init(){
		if(bootstrap != null) return;
		if(this.group == null){
			throw new IllegalStateException("group missing");
		}
		bootstrap = new Bootstrap();
		bootstrap.group(this.group) 
		 .channel(NioSocketChannel.class)  
		 .handler(new ChannelInitializer<SocketChannel>() { 
			NettyAdaptor nettyToIoAdaptor = new NettyAdaptor(ioAdaptor);
			@Override
			protected void initChannel(SocketChannel ch) throws Exception { 
				if(codecInitializer == null){
					log.warn("Missing codecInitializer"); 
				} 
				ChannelPipeline p = ch.pipeline();
				if(sslCtx != null){
					p.addLast(sslCtx.newHandler(ch.alloc()));
				}
				if(codecInitializer != null){
					List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
					codecInitializer.initPipeline(handlers);
					for(ChannelHandler handler : handlers){ 
						p.addLast((ChannelHandler)handler);
					}
				}
				p.addLast(nettyToIoAdaptor);
			}
		});  
	}   
	 
	
	public synchronized void startHeartbeat(long intervalInMillis, MessageBuilder<REQ> builder){
		this.heartbeatMessageBuilder = builder;
		if(heartbeator == null){
			heartbeator = Executors.newSingleThreadScheduledExecutor();
			this.heartbeator.scheduleAtFixedRate(()-> { 
				try {
					if(heartbeatMessageBuilder != null){
						REQ msg = heartbeatMessageBuilder.build();
						sendMessage(msg);
					}
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				} 
			}, intervalInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
		}
	}  

	public ConnectionStatus getStatus() {
		return status;
	} 
	
	public void sendMessage(REQ req) { 
		if(status != ConnectionStatus.Open){
			sendingMessages.add(req);
			if(status == ConnectionStatus.New){
				connect();
			}
			return;
		}
		session.write(req);  
    } 
	
	 
	@Override
	public void close() throws IOException {
		onOpen = null;
		onClose = null; 
		
		if(session != null){
			session.close();
			session = null;
		}   
		
		if(heartbeator != null){
			heartbeator.shutdownNow();
			heartbeator = null;
		} 
		
		status = ConnectionStatus.Closed;
	}   
}
