package org.zbus.proxy;
import java.io.IOException;

import org.zbus.net.IoAdaptor;
import org.zbus.net.Session; 
 
/**
 * Transparently proxy TCP package from local server to target server
 * 
 * @author rushmore (洪磊明)
 *
 */
public class TcpProxy implements IoAdaptor{
	private static final String CHAIN_KEY = "chain";
	protected String targetAddress;
	
	public TcpProxy(String targetAddress) {
		this.targetAddress = targetAddress;
	}
	
	@Override
	public void onSessionAccepted(Session sess) throws IOException {
		Session target = null; 
		sess.attr(CHAIN_KEY, target); 
		//target.attr(CHAIN_KEY,sess); 
		//FIXME connect to target
	}
	
	@Override
	public void onSessionConnected(Session sess) throws IOException {  
		Session chain = sess.attr(CHAIN_KEY);
		if(chain == null){ 
			sess.asyncClose();
			return; 
		}    
		//FIXME
	}

	@Override
	public void onMessage(Object msg, Session sess) throws IOException {  
		Session chain = sess.attr(CHAIN_KEY);
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
		Session chain = sess.attr(CHAIN_KEY);
		if (chain == null) return; 
		try {	
			chain.close();	
			chain.attr(CHAIN_KEY, null);
			sess.attr(CHAIN_KEY, null);
		} catch (IOException e) { 
		}
	}	

	@Override
	public void onSessionRegistered(Session sess) throws IOException {
		
	}
	@Override
	public void onException(Throwable e, Session sess) throws Exception {
	}
	
	
	public static void main(String[] args) throws Exception {   
		
	}
}
