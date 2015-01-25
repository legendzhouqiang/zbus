package org.zbus.client;

import java.io.IOException;

import org.zbus.remoting.Message;
import org.zbus.remoting.RemotingClient;
import org.zbus.remoting.ticket.ResultCallback;



public interface Broker {
	/**
	 * 创建一个消费者连接
	 * @param hint
	 * @return
	 */
	RemotingClient getConsumerClient(ClientHint hint) throws IOException;
	/**
	 * 关闭一个消费者连接
	 * @param client
	 */
	void closeConsumerClient(RemotingClient client) throws IOException;
	void produceMessage(Message msg, final ResultCallback callback) throws IOException;
	Message produceMessage(Message req, int timeout) throws IOException;  
	void destroy();
}
