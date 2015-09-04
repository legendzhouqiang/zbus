package org.zbus.net.pool;

import org.zbus.kit.pool.ObjectFactory;
import org.zbus.kit.pool.Pool;
import org.zbus.kit.pool.PoolConfig;


public class PoolExample {
	
	public static void main(String[] args) throws Exception { 
		PoolConfig config = new PoolConfig();
		config.setMaxTotal(8);
		
		ObjectFactory<String> factory = new ObjectFactory<String>() {
			@Override
			public String createObject() throws Exception { 
				return "string"+System.currentTimeMillis();
			}

			@Override
			public void destroyObject(String obj) {
			}

			@Override
			public boolean validateObject(String obj) {
				return true;
			}
			
		};
		
		//Pool<String> pool = new DefaultPool<String>(factory, config);
		//Pool.setPoolFactory(new DefaultPoolFactory());
		
		Pool<String> pool = Pool.getPool(factory, config);
		
		for(int i=0;i<10;i++){
			String s = pool.borrowObject();
			System.out.println(s);
			pool.returnObject(s);
			
		}
		
		pool.close();
	} 
}
