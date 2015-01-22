package org.zbus.client;

import org.zbus.remoting.RemotingClient;



public interface Broker {
	/**
	 * 从broker获取一个链接，可以是新创建的，也可以是连接池中借用的
	 * @param hint
	 * @return
	 */
	RemotingClient getClient(ClientHint hint);
	
	/**
	 * 关闭链接，底层可以实现为关闭物理连接，也可以是返回连接池
	 */
	void closeClient(RemotingClient client);
	
	/**
	 * 销毁Broker
	 */
	void destroy();
}
