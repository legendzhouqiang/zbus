package org.zbus.net;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.IoBuffer;
import org.zbus.net.core.Session;
import org.zbus.proxy.TcpProxyServer;

public class TcpProxyAdaptor extends IoAdaptor {
	private String targetAddress;

	public TcpProxyAdaptor(String targetAddress) {
		this.targetAddress = targetAddress;
	}

	// 透传不需要编解码，简单返回ByteBuffer数据
	public IoBuffer encode(Object msg) {
		if (msg instanceof IoBuffer) {
			IoBuffer buff = (IoBuffer) msg;
			return buff;
		} else {
			throw new RuntimeException("Message Not Support");
		}
	}

	// 透传不需要编解码，简单返回ByteBuffer数据
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
		Dispatcher dispatcher = sess.getDispatcher();
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
		Dispatcher dispatcher = new Dispatcher(); 
		IoAdaptor ioAdaptor = new TcpProxyServer("10.17.2.30:3306"); 
		final Server server = new Server(dispatcher, ioAdaptor, 3306);
		server.setServerName("TcpProxyServer");
		server.start();
	}
}
