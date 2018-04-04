package io.zbus.mq.model;

public interface MessageDispatcher {  
	void dispatch(Channel channel);
	void dispatch(Domain domain);
}
