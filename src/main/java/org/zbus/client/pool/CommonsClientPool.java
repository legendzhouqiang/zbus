package org.zbus.client.pool;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.zbus.client.ClientPool;
import org.zbus.remoting.ClientDispatcherManager;
import org.zbus.remoting.RemotingClient;

public class CommonsClientPool implements ClientPool{
	private final ObjectPool<RemotingClient> pool;
	private final GenericObjectPoolConfig config;
	private final String broker;
	
	public CommonsClientPool(GenericObjectPoolConfig config, String broker, ClientDispatcherManager clientMgr){
		this.config = config;
		this.broker = broker;
		RemotingClientFactory factory = new RemotingClientFactory(clientMgr, this.broker);	 
		this.pool = new GenericObjectPool<RemotingClient>(factory, this.config);
	}
	
	public CommonsClientPool(GenericObjectPoolConfig config, String broker) throws IOException {
		this(config, broker, defaultClientDispachterManager());
	}  
	
	public CommonsClientPool(PoolConfig config, String broker) throws IOException {
		this(config, broker, null);
	}  
	
	public CommonsClientPool(PoolConfig config, String broker, ClientDispatcherManager clientMgr){
		this(toObjectPoolConfig(config), broker, clientMgr);
	}
	
	private static ClientDispatcherManager defaultClientDispachterManager() throws IOException{
		ClientDispatcherManager clientMgr = new ClientDispatcherManager();
		clientMgr.start();
		return clientMgr;
	}
	
	private static GenericObjectPoolConfig toObjectPoolConfig(PoolConfig config){
		//TODO
		return new GenericObjectPoolConfig();
	}
	
 
	public RemotingClient borrowClient(String mq) throws Exception {
		return pool.borrowObject();
	}

	public List<RemotingClient> borrowEachClient(String mq) throws Exception {
		return Arrays.asList(borrowClient(mq));
	}
	
	public void invalidateClient(RemotingClient client) throws Exception{
		pool.invalidateObject(client);
	}
	
	public void returnClient(RemotingClient client) throws Exception {
		pool.returnObject(client);  
	}
	
	public void returnClient(List<RemotingClient> clients) throws Exception { 
		for(RemotingClient client : clients){
			returnClient(client);
		}
	}
	
	
	public void destroy() { 
		pool.close();
	}

	
}

class RemotingClientFactory extends BasePooledObjectFactory<RemotingClient> {
	
	private final ClientDispatcherManager cliengMgr;
	private final String broker; 
	
	public RemotingClientFactory(final ClientDispatcherManager clientMgr, final String broker){
		this.cliengMgr = clientMgr;
		this.broker = broker;
	}
	
	
	@Override
	public RemotingClient create() throws Exception { 
		return new RemotingClient(broker, cliengMgr);
	}

	@Override
	public PooledObject<RemotingClient> wrap(RemotingClient obj) { 
		return new DefaultPooledObject<RemotingClient>(obj);
	} 
}