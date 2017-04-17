package io.zbus.mq;

import java.io.IOException;

public interface ConsumeHandler{
	void handle(Message msg, MqClient client) throws IOException;
}