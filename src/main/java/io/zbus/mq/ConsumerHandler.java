package io.zbus.mq;

import java.io.IOException;

public interface ConsumerHandler{
	void handle(Message msg, Consumer consumer) throws IOException;
}