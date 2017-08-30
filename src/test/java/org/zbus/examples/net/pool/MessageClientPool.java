package org.zbus.examples.net.pool;

import org.zbus.kit.pool.Pool;
import org.zbus.kit.pool.PoolConfig;
import org.zbus.net.http.MessageClient;
import org.zbus.net.http.MessageClientFactory;

public class MessageClientPool {

	public static void main(String[] args) throws Exception { 
		MessageClientFactory factory = new MessageClientFactory("127.0.0.1:15555");
		
		PoolConfig config = new PoolConfig();
		Pool<MessageClient> pool = Pool.getPool(factory, config);
		
		MessageClient client = pool.borrowObject();
		pool.returnObject(client);
		
		
		factory.close();
		pool.close();
	}

}
