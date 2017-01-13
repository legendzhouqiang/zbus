package io.zbus.mq;

public interface MessageProcessor { 
	Message process(Message request);
}