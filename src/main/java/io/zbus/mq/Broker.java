package io.zbus.mq;
 
import java.io.Closeable;
import java.io.IOException;
import java.util.List;


public interface Broker extends Closeable{  
	
	MessageInvoker selectForProducer(String topic) throws IOException; 
	
	MessageInvoker selectForConsumer(String topic) throws IOException;  
	
	void releaseInvoker(MessageInvoker invoker) throws IOException; 
	
	List<Broker> availableServerList();
	
	void onSelect(ServerSelector selector);
	
	void registerServer(String serverAddress);
	
	void unregisterServer(String serverAddress);
	
	void addServerListener(ServerNotifyListener listener);
	
	void removeServerListener(ServerNotifyListener listener); 
	
	
	public static interface ServerSelector {

	}
	
	public static interface ServerNotifyListener{
		
	}
}
