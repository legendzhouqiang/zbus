package org.zbus.mq.server.support;

import java.util.concurrent.LinkedBlockingQueue;

import org.zbus.net.http.Message;

public class MessageMemoryQueue extends LinkedBlockingQueue<Message> {
	private static final long serialVersionUID = -1506102218456203704L;
}
