package io.zbus.mq.model;

public interface MessageRepository {
	Domain getDomain(String name);
	void addDomain(Domain domain);
	void removeDomain(String domain);
	void updateDomain(Domain domain);
	
	Channel getChannel(String channelId);
	void addChannel(Channel channel);
	void removeChannel(String channelId);
	void updateChannel(Channel channel);
	
	void write(Object message);
	Object read(Channel channel); 
}
