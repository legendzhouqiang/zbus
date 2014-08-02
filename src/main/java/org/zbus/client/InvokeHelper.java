package org.zbus.client;

import java.io.IOException;
import java.util.List;

import org.logging.Logger;
import org.logging.LoggerFactory;
import org.remoting.Message;
import org.remoting.RemotingClient;
import org.remoting.ticket.ResultCallback;

public class InvokeHelper { 
	private static final Logger log = LoggerFactory.getLogger(InvokeHelper.class); 
	
	
	
	
	public static void invokeAsync(ClientPool pool, RemotingClient client,
			Message msg, final ResultCallback callback) throws IOException{
		
		if(client == null && pool == null){
			throw new IllegalArgumentException("Client/Agent both null");
		} 
		
		if(client != null){
			client.invokeAsync(msg, callback); 
			return;
		}
		
		try{
			final String mq = msg.getMq();
			client = pool.borrowClient(mq);  
			client.invokeAsync(msg, callback);
			
		} catch (Exception e){
			try {
				pool.invalidateClient(client);
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
			throw new ZbusException(e.getMessage(), e);
		} finally{
			if(client != null){
				try {
					pool.returnClient(client);
				} catch (Exception e) { 
					e.printStackTrace();
				}
			}
		}  
	} 
	
	
	public static void invokeAsyncAll(ClientPool pool, RemotingClient client,
			Message msg, final ResultCallback callback) throws IOException{
			
		if(client == null && pool == null){
			throw new IllegalArgumentException("Client/Agent both null");
		}
		
		final String mq = msg.getMq();
		if(client != null){
			client.invokeAsync(msg, callback);  
			return;
		}
		
		List<RemotingClient> clientList = null;
		try{
			clientList = pool.borrowEachClient(mq);  
			try{
				for(RemotingClient cli : clientList){
					cli.invokeAsync(msg, callback);
				}
			} catch(Exception ex) {
				log.error(ex.getMessage(), ex);
			} 
		} catch (Exception e){
			throw new ZbusException(e.getMessage(), e);
		} finally{
			if(clientList != null){
				try {
					pool.returnClient(clientList);
				} catch (Exception e) { 
					e.printStackTrace();
				}
			}
		}  
	} 
	
	public static Message invokeSync(ClientPool pool, RemotingClient client,
			Message msg, int timeout) throws IOException{
			
		if(client == null && pool == null){
			throw new IllegalArgumentException("Client/Agent both null");
		}
		
		final String mq = msg.getMq();
		if(client != null){
			return client.invokeSync(msg, timeout);
		}
    	
		try{
			client = pool.borrowClient(mq);  
			return client.invokeSync(msg, timeout);
			
		} catch (Exception e){
			throw new ZbusException(e.getMessage(), e);
		} finally{
			if(client != null){
				try {
					pool.returnClient(client);
				} catch (Exception e) { 
					e.printStackTrace();
				}
			}
		}  
	} 
}
