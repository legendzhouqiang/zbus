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
import io.netty.channel.EventLoopGroup;
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
	protected final URI uri;

	protected Bootstrap bootstrap;
	protected final EventLoopGroup group;
	protected SslContext sslCtx;
	private SSLEngine sslEngine; // FIXME use SslContext

	protected ChannelFuture channelFuture;
	protected CodecInitializer codecInitializer;

	protected volatile ScheduledExecutorService heartbeator = null;
	protected MessageBuilder<REQ> heartbeatMessageBuilder;

	protected Session session;
	protected IoAdaptor ioAdaptor;

	protected CountDownLatch activeLatch = new CountDownLatch(1);
	protected List<REQ> messageSendingQueue = Collections.synchronizedList(new ArrayList<>());

	private ConnectionStatus status = ConnectionStatus.New;
 
	public Client(String address, EventLoop loop) {
		group = loop.getGroup();
		boolean isSsl = false;
		try {
			uri = new URI(address); 
			String scheme = uri.getScheme();
			host = uri.getHost();
			port = uri.getPort();
			isSsl = "https".equalsIgnoreCase(scheme) || "wss".equals(scheme);
			if(port < 0){
				port = isSsl? 443 : 80;
			}
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(address + " is illegal");
		}   
		
		sslCtx = loop.getSslContext(); 
		if (isSsl && sslCtx == null) {
			sslEngine = loop.getSSLEngine(host, port);
		}

		EventHandler reconnect = ()->{
			log.warn("Trying to reconnect to (%s) in %.1f seconds", serverAddress(), reconnectDelay / 1000.0);
			try {
				Thread.sleep(reconnectDelay);
			} catch (InterruptedException e1) {
				return;
			} 
			connect();
		};
		
		onOpen = () -> {
			String msg = String.format("Connection(%s) OK", serverAddress());
			log.info(msg);
		};

		onClose = () -> {
			log.warn("Disconnected from(%s)", serverAddress());
			reconnect.handle();
		};

		onError = e -> {
			log.error(e.getMessage(), e);
			reconnect.handle();
		}; 

		ioAdaptor = new IoAdaptor() {
			@Override
			public void sessionCreated(Session session) throws IOException {
				Client.this.session = session;
				activeLatch.countDown();
				status = ConnectionStatus.Open;
				for (REQ req : messageSendingQueue) {
					session.write(req);
				}
				messageSendingQueue.clear();

				if (onOpen != null) {
					onOpen.handle();
				}
			}

			public void sessionToDestroy(Session session) throws IOException {
				if (Client.this.session != null) {
					Client.this.session.close();
					Client.this.session = null;
				}
				status = ConnectionStatus.Closed;
				if (onClose != null) {
					onClose.handle();
				}
			}

			@Override
			public void onError(Throwable e, Session sess) {
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

	protected String serverAddress() {
		return String.format("%s://%s:%d", uri.getScheme(), host, port);
	}

	public void codec(CodecInitializer codecInitializer) {
		this.codecInitializer = codecInitializer;
	}

	public synchronized void connect() {
		init();
		if(status == ConnectionStatus.Connecting){
			log.info("Connecting to (%s) in process", serverAddress());
			return;
		}
		
		activeLatch = new CountDownLatch(1);
		status = ConnectionStatus.Connecting;
		if(channelFuture != null){
			if(channelFuture.channel() != null)
				channelFuture.channel().close();
		}
		
		channelFuture = bootstrap.connect(host, port);
		try {
			channelFuture = channelFuture.sync(); 
		} catch (InterruptedException e) {
			return;
		} catch (Throwable ex) {
			if(channelFuture != null){
				if(channelFuture.channel() != null)
					channelFuture.channel().close();
			}
			
			if (onClose != null) {
				onClose.handle();
			}

			if (onError != null) {
				try {
					onError.handle(ex);
				} catch (Exception e) {
					if (ex instanceof RuntimeException) {
						throw (RuntimeException) ex;
					}
					throw new RuntimeException(ex.getMessage(), ex.getCause());
				}
			} else {
				if (ex instanceof RuntimeException) {
					throw (RuntimeException) ex;
				}
				throw new RuntimeException(ex.getMessage(), ex.getCause());
			}
		}
	}  

	private void init() {
		if (bootstrap != null)
			return;
		if (this.group == null) {
			throw new IllegalStateException("group missing");
		}
		bootstrap = new Bootstrap();
		bootstrap.group(this.group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
			NettyAdaptor nettyToIoAdaptor = new NettyAdaptor(ioAdaptor);

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				if (codecInitializer == null) {
					log.warn("Missing codecInitializer");
				}
				ChannelPipeline p = ch.pipeline();
				if (sslCtx != null) {
					p.addLast(sslCtx.newHandler(ch.alloc()));
				} else if (sslEngine != null) { // FIXME use only sslCtx
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

	public synchronized void startHeartbeat(long intervalInMillis, MessageBuilder<REQ> builder) {
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

	public ConnectionStatus getStatus() {
		return status;
	}

	public void sendMessage(REQ req) {
		if (status != ConnectionStatus.Open) {
			messageSendingQueue.add(req);
			if (status == ConnectionStatus.New) {
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

		if (session != null) {
			session.close();
			session = null;
		}

		if (heartbeator != null) {
			heartbeator.shutdownNow();
			heartbeator = null;
		}

		status = ConnectionStatus.Closed;
	}
}
