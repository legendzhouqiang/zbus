package org.zbus.proxy;
import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.zbus.net.core.SelectorGroup;
import org.zbus.kit.ConfigKit;
import org.zbus.net.Server;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.IoBuffer;
import org.zbus.net.core.Session;
 
/**
 * Transparently proxy TCP package from local server to target server
 * 
 * @author rushmore (洪磊明)
 *
 */
public class TcpProxy extends IoAdaptor{
	private String targetAddress;
	
	public TcpProxy(String targetAddress) {
		this.targetAddress = targetAddress;
	}
	public IoBuffer encode(Object msg) {
		if (msg instanceof IoBuffer) {
			IoBuffer buff = (IoBuffer) msg;
			return buff;
		} else {
			throw new RuntimeException("Message Not Support");
		}
	}
	public Object decode(IoBuffer buff) {
		if (buff.remaining() > 0) {
			byte[] data = new byte[buff.remaining()];
			buff.readBytes(data);
			return IoBuffer.wrap(data);
		} else {
			return null;
		}
	}

	@Override
	protected void onSessionAccepted(Session sess) throws IOException {
		Session target = null;
		SelectorGroup dispatcher = sess.getDispatcher();
		try {
			target = dispatcher.createClientSession(targetAddress, this);
		} catch (Exception e) {
			sess.asyncClose();
			return;
		}
		sess.chain = target;
		target.chain = sess;
		dispatcher.registerSession(SelectionKey.OP_CONNECT, target);
	}
	
	@Override
	public void onSessionConnected(Session sess) throws IOException {  
		Session chain = sess.chain;
		if(chain == null){ 
			sess.asyncClose();
			return; 
		}   
		if(sess.isActive() && chain.isActive()){ 
			sess.register(SelectionKey.OP_READ);
			chain.register(SelectionKey.OP_READ);
		}
	}

	@Override
	protected void onMessage(Object msg, Session sess) throws IOException {  
		Session chain = sess.chain;
		if(chain == null){
			sess.asyncClose(); 
			return;
		} 
		chain.write(msg); 
	}
	
	@Override
	public void onSessionToDestroy(Session sess) throws IOException {   
		try {
			sess.close();
		} catch (IOException e) { //ignore
		} 
		if (sess.chain == null) return; 
		try {	
			sess.chain.close();	
			sess.chain.chain = null;
			sess.chain = null;
		} catch (IOException e) { 
		}
	}	
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {   
		SelectorGroup selectorGroup = new SelectorGroup(); 
		final Server server = new Server(selectorGroup); 
		String target = ConfigKit.option(args, "-target", "127.0.0.1:3306");
		int port = ConfigKit.option(args, "-port", 33060);
		
		server.start(port, new TcpProxy(target));
	}
}
