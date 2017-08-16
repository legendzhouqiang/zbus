package io.zbus.mq.server;

import io.zbus.mq.Message;
import io.zbus.transport.Client.MsgHandler;

public interface MessageHandler extends MsgHandler<Message> { }