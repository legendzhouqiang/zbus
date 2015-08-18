package org.zbus.proxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.zbus.kit.ConfigKit;
import org.zbus.log.Logger;
import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;
 
public class TcpProxyAdaptor extends BindingAdaptor{ 
	private static final Logger log = Logger.getLogger(TcpProxyAdaptor.class);   
	
	private String targetAddress;   
	
    public TcpProxyAdaptor(String targetAddress){ 
    	codec(new ProxyCodec());  
        this.targetAddress = targetAddress;
    } 
    
    @Override
    protected void onSessionAccepted(Session sess) throws IOException {
    	if(log.isDebugEnabled()){
    		log.debug("Bind upstream: %s", sess); 
    		log.debug("Try to connect downstream(%s)", targetAddress);
    	}
    	
    	Session target = null;
    	Dispatcher dispatcher = sess.getDispatcher();
    	try{ 
	    	target = dispatcher.createClientSession(targetAddress, this); 
    	} catch (Exception e){ 
    		log.error("Reject upstream connection: %s", sess);
    		log.error(e.getMessage(), e);
    		
    		sess.asyncClose();
    		return;
    	}
    	
    	dispatcher.registerSession(SelectionKey.OP_CONNECT, target);  
    	sess.chain = target;
    	target.chain = sess; 
    } 
    
	public static void main(String[] args) throws Exception {  
		int serverPort = ConfigKit.option(args, "-server", 80);
		String target = ConfigKit.option(args, "-target", "127.0.0.1:15555");
		Dispatcher dispatcher = new Dispatcher();
		
		IoAdaptor ioAdaptor = new TcpProxyAdaptor(target);
		
		@SuppressWarnings("resource")
		Server server = new Server(dispatcher, ioAdaptor, serverPort);
		server.setServerName("TcpProxyServer");
		server.start();
	}
}
