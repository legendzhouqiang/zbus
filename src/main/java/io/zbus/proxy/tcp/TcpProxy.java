package io.zbus.proxy.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.zbus.kit.ConfigKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.tcp.TcpServer;

public class TcpProxy extends ServerAdaptor implements Closeable {
	protected static final Logger log = LoggerFactory.getLogger(TcpProxy.class);
	private int port; 
	private String targetHost;
	private int targetPort = 80;
	private TcpServer server;   
	
	public TcpProxy(int port, String target) { 
		this.port = port;
		String[] bb = target.split("[:]");
		if(bb.length > 0){
			this.targetHost = bb[0].trim();
		}
		if(bb.length > 1){
			this.targetPort = Integer.valueOf(bb[1].trim());
		} 
	}
	
	@Override
	public void sessionCreated(Session sess) throws IOException { 
		super.sessionCreated(sess); //add to session table
		
		ProxyClient client = new ProxyClient(sess, targetHost, targetPort, server.getEventLoop()); 
		sess.attr("downClient", client);
	} 
	 
	
	protected void cleanSession(Session sess){
		try { 
			ProxyClient client = sess.attr("downClient");
			if(client != null){
				client.close();
			}
			super.cleanSession(sess);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		Session down = sess.attr("down");
		if(down == null){
			Queue<Object> delayed = sess.attr("delayed");
			if(delayed == null){
				delayed = new ConcurrentLinkedQueue<Object>();
			}
			sess.attr("delayed", delayed);
			delayed.add(msg);
		} else {
			down.write(msg);
		}
	} 
	
	@Override
	public void close() throws IOException { 
		server.close(); 
	}
	
	public void start(){
		server = new TcpServer();   
		server.start(port, this);
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		int port = ConfigKit.option(args, "-p", 80);
		String target = ConfigKit.option(args, "-t", "127.0.0.1:15555"); 
		
		TcpProxy proxy = new TcpProxy(port, target);
		proxy.start();
	} 
}
