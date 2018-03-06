package io.zbus.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class Client<REQ, RES> implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Client.class);

	public volatile MessageHandler<RES> onMessage;
	public volatile ErrorHandler onError;
	public volatile EventHandler onOpen;
	public volatile EventHandler onClose;

	public long connectTimeout = 3000;
	public long reconnectDelay = 3000;

	protected String host;
	protected int port; 
	protected URI uri;

	protected Bootstrap bootstrap;
	protected Object bootstrapLock = new Object(); 
	protected final EventLoop loop;
	protected SslContext sslCtx; 
	protected boolean sslEnabled;

	protected ChannelFuture connectFuture;
	protected CodecInitializer codecInitializer;

	protected volatile ScheduledExecutorService heartbeator = null;
	protected MessageBuilder<REQ> heartbeatMessageBuilder;
	protected volatile ScheduledExecutorService reconnectRunner = Executors.newSingleThreadScheduledExecutor();

	protected Session session;
	protected Object sessionLock = new Object();
	protected IoAdaptor ioAdaptor;

	protected CountDownLatch activeLatch = new CountDownLatch(1);
	protected List<REQ> messageSendingQueue = Collections.synchronizedList(new ArrayList<>()); 
	protected boolean triggerOpenWhenConnected = true;
	
	public Client(URI uri, EventLoop loop) {
		this.uri = uri;
		this.loop = loop;
		setup();
	}
	
	public Client(String address, EventLoop loop) {
		this.loop = loop;
		try {
			this.uri = new URI(address);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(address + " is illegal");
		}
		setup();
	}
	
	private void setup() {  
		String scheme = uri.getScheme();
		host = uri.getHost();
		port = uri.getPort();
		sslEnabled = "https".equalsIgnoreCase(scheme) || "wss".equals(scheme);
		if(port < 0){
			port = sslEnabled? 443 : 80;
		}  
		sslCtx = loop.getSslContext();  
 
		onClose = () -> {
			log.warn("Trying to reconnect to (%s) in %.1f seconds", serverAddress(), reconnectDelay / 1000.0); 
			reconnectRunner.schedule(()->{
				if(!active()){
					connect();
				}
			}, reconnectDelay, TimeUnit.MILLISECONDS); 
		};  

		ioAdaptor = new IoAdaptor() {
			@Override
			public void sessionCreated(Session session) throws IOException {
				synchronized (Client.this.sessionLock) {
					Client.this.session = session;
				} 
				activeLatch.countDown(); 
				
				String msg = String.format("Connection(%s) OK", serverAddress());
				log.info(msg);
				
				if(triggerOpenWhenConnected){
					sendCachedMessages();
					if (onOpen != null) {
						onOpen.handle();
					}
				}  
			}

			public void sessionToDestroy(Session session) throws IOException { 
				cleanSession();  
				if (onClose != null) {
					onClose.handle();
				}
			}

			@Override
			public void onError(Throwable e, Session sess) { 
				cleanSession(); //trigger sessionToDestroy
				
				if (onError != null) {
					try {
						onError.handle(e);
					} catch (Exception ex) {
						log.error(ex.getMessage(), ex.getCause());
					}
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
				RES res = (RES) msg;
				if (onMessage != null) {
					onMessage.handle(res);
					return;
				}
				log.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", res);
			}
		};
	}
	
	protected void sendCachedMessages(){ 
		for (REQ req : messageSendingQueue) {
			session.write(req);
		}
		messageSendingQueue.clear();
	}

	protected String serverAddress() {
		return String.format("%s://%s:%d%s", uri.getScheme(), host, port, uri.getPath());
	} 

	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	}

	public synchronized void connect() {
		init(); 
		synchronized (sessionLock) {
			if(connectFuture != null){
				log.info("Connecting to (%s) in process", serverAddress());
				return;
			}
			activeLatch = new CountDownLatch(1);   
			connectFuture = bootstrap.connect(host, port);
			try {
				connectFuture = connectFuture.sync(); 
			} catch (Throwable ex) { 
				cleanSessionUnsafe();
				log.error(ex.getMessage(), ex);
				if(onClose != null){
					onClose.handle();
				}
			}
		} 
	}  

	private void init() {
		synchronized (bootstrapLock) {
			if (bootstrap != null) return; 
			if (this.loop.getGroup() == null) {
				throw new IllegalStateException("group missing");
			}
			
			bootstrap = new Bootstrap();
			bootstrap.group(this.loop.getGroup())
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() { 
					NettyAdaptor nettyToIoAdaptor = new NettyAdaptor(ioAdaptor);
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						if (codecInitializer == null) {
							log.warn("Missing codecInitializer");
						}
						ChannelPipeline p = ch.pipeline();
						if (sslCtx != null) { //SslContext take first priority!!!
							p.addLast(sslCtx.newHandler(ch.alloc()));
						} else if (sslEnabled) {
							SSLEngine sslEngine = loop.buildSSLEngine(host, port, ch.alloc());
							SslHandler sslHandler = new SslHandler(sslEngine);
							p.addLast(sslHandler);
						}
						
						if (codecInitializer != null) {
							List<ChannelHandler> handlers = new ArrayList<ChannelHandler>();
							codecInitializer.initPipeline(handlers);
							for (ChannelHandler handler : handlers) {
								p.addLast((ChannelHandler) handler);
							}
						} 
						p.addLast(nettyToIoAdaptor);
					}
				});
		}
	}

	public synchronized void heartbeat(long intervalInMillis, MessageBuilder<REQ> builder) {
		this.heartbeatMessageBuilder = builder;
		if (heartbeator == null) {
			heartbeator = Executors.newSingleThreadScheduledExecutor();
			this.heartbeator.scheduleAtFixedRate(() -> {
				try {
					if (heartbeatMessageBuilder != null) {
						REQ msg = heartbeatMessageBuilder.build();
						sendMessage(msg);
					}
				} catch (Exception e) {
					log.warn(e.getMessage(), e);
				}
			}, intervalInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
		}
	} 
	
	public boolean active(){ 
		synchronized (sessionLock) {
			return session != null && session.active();
		} 
	}

	public void sendMessage(REQ req) {
		if(!active()){
			messageSendingQueue.add(req);  
			if(connectFuture == null){
				connect();
			}
			return;
		} 
		session.write(req);
	}

	protected void cleanSession() { 
		synchronized (sessionLock) {
			cleanSessionUnsafe();
		} 
	}
	
	protected void cleanSessionUnsafe() { 
		if (session != null) {
			try {
				session.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e.getCause());
			}
			session = null;
		}
		if(connectFuture != null && connectFuture.channel() != null){
			try {
				connectFuture.channel().close(); 
			} catch (Exception e) {
				log.error(e.getMessage(), e.getCause());
			}
			
		} 
		connectFuture = null;
	}
	
	@Override
	public void close() throws IOException {
		onOpen = null;
		onClose = null;

		cleanSession();

		if (heartbeator != null) {
			heartbeator.shutdownNow();
			heartbeator = null;
		} 
		if (reconnectRunner != null) {
			reconnectRunner.shutdownNow();
			reconnectRunner = null;
		} 
	}
}
