package io.zbus.net;
 
import java.io.Closeable;
import java.io.IOException;
   

public interface Client<REQ, RES> extends IoAdaptor, Closeable { 

	boolean hasConnected();
	void connectAsync() throws IOException; 
	void connectSync(long timeout) throws IOException, InterruptedException;
	void ensureConnectedAsync(); 
	
	void sendMessage(REQ req) throws IOException, InterruptedException;; 
	void onMessage(MessageHandler<RES> messageHandler);
	void onError(ErrorHandler errorHandler);
    void onConnected(ConnectedHandler connectedHandler);
    void onDisconnected(DisconnectedHandler disconnectedHandler);
    
    <V> V attr(String key);
	<V> void attr(String key, V value);  
}
