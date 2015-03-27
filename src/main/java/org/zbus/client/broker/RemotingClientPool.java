package org.zbus.client.broker;

import java.io.IOException;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.zbus.remoting.ClientDispatcherManager;
import org.zbus.remoting.RemotingClient;

public class RemotingClientPool extends GenericObjectPool<RemotingClient>{
	public RemotingClientPool(ClientDispatcherManager clientMgr, String broker, GenericObjectPoolConfig config) throws IOException{
		super(new RemotingClientFactory(clientMgr, broker), config);
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
	
	@Override
	public void destroyObject(PooledObject<RemotingClient> p) throws Exception {
		RemotingClient client = p.getObject();
		client.close();
	}
	
	@Override
	public boolean validateObject(PooledObject<RemotingClient> p) {
		return p.getObject().hasConnected();
	}
}