package io.zbus.net;
 
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
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


public class Client<REQ, RES> extends AttributeMap implements IoAdaptor, Closeable{
	private static final Logger log = LoggerFactory.getLogger(Client.class); 
	  
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
	protected String clientId; 
	protected int connectTimeout = 3000;  
	
	protected CountDownLatch activeLatch = new CountDownLatch(1);     
	
	public volatile MessageHandler<RES> messageHandler; 
	public volatile ErrorHandler errorHandler;
	public volatile ConnectedHandler connectedHandler;
	public volatile DisconnectedHandler disconnectedHandler;   

	
	public Client(String address, EventLoop loop){   
		group = loop.getGroup(); 
		
		Object[] hp = NetKit.hostPort(address);
		this.host = (String)hp[0];
		this.port = (Integer)hp[1]; 
		
		connectedHandler = ()->{
			String msg = String.format("Connection(%s) OK, ID=%s", serverAddress(), clientId);
			log.info(msg); 
		};
		
		disconnectedHandler = ()->{
			log.warn("Disconnected from(%s) ID=%s", serverAddress(), clientId);
			ensureConnected();//automatically reconnect by default
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
		channelFuture = bootstrap.connect(host, port);
	}   
	
	
	public void connectSync(long timeout) throws IOException, InterruptedException {
		if(hasConnected()) return; 
		
		synchronized (this) {
			if(!hasConnected()){ 
	    		connect();
	    		activeLatch.await(timeout,TimeUnit.MILLISECONDS); 
				
	    		if(hasConnected()){ 
					return;
				}   
				channelFuture.sync();
				String msg = String.format("Connection(%s) failed", serverAddress()); 
				log.warn(msg);
				cleanSession();  
			}
		} 
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
			NettyAdaptor nettyToIoAdaptor = new NettyAdaptor(Client.this);
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
	
	
	protected synchronized void cleanSession() throws IOException{
		if(session != null){
			session.close();
			session = null;
			activeLatch = new CountDownLatch(1);
		} 
	}
	 
	
	public boolean hasConnected() {
		return session != null && session.active();
	}
	
	private Thread asyncConnectThread; 
	public void ensureConnected(){
		if(hasConnected()) return;
		if(asyncConnectThread != null) return;
		
		asyncConnectThread = new Thread(()->{  
			try {
				while(!hasConnected()){
					try{
						connectSync(connectTimeout); 
						if(!hasConnected()){
							Thread.sleep(connectTimeout);
						}
					} catch (IOException e) {    
						String msg = String.format("Trying again(%s) in %.1f seconds", serverAddress(), connectTimeout/1000.0); 
						log.warn(msg); 
						Thread.sleep(connectTimeout);
					} catch (InterruptedException e) {
						throw e;
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						break;
					}  
				} 
				asyncConnectThread = null;
			} catch (InterruptedException e) {
				//ignore
			}   
		}); 
		asyncConnectThread.start(); 
	}
	
	
	public void sendMessage(REQ req) throws IOException, InterruptedException{
		if(!hasConnected()){
			connectSync(connectTimeout);  
			if(!hasConnected()){
				String msg = String.format("Connection(%s) failed", serverAddress()); 
				throw new IOException(msg);
			}
		}  
		session.write(req);  
    } 
	
	 
	@Override
	public void close() throws IOException {
		connectedHandler = null;
		disconnectedHandler = null;
		
		if(asyncConnectThread != null){
			asyncConnectThread.interrupt();
			asyncConnectThread = null;
		}
		
		if(session != null){
			session.close();
			session = null;
		}   
		
		if(heartbeator != null){
			heartbeator.shutdownNow();
			heartbeator = null;
		} 
	} 
  

	@Override
	public void sessionCreated(Session session) throws IOException { 
		this.session = session;
		this.clientId = this.session.id();
		activeLatch.countDown();
		if(connectedHandler != null){
			connectedHandler.onConnected();
		}
	}

	public void sessionToDestroy(Session session) throws IOException {
		if(this.session != null){
			this.session.close(); 
			this.session = null;
		} 
		
		if(disconnectedHandler != null){
			disconnectedHandler.onDisconnected();
		}   
	} 

	@Override
	public void onError(Throwable e, Session sess) throws IOException { 
		if(errorHandler != null){
			errorHandler.onError(e, session);
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
    	if(messageHandler != null){
    		messageHandler.handle(res, sess);
    		return;
    	} 
    	
    	log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", res);
	}    
}
