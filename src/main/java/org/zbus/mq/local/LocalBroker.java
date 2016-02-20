package org.zbus.mq.local;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.zbus.broker.Broker;
import org.zbus.kit.log.Logger;
import org.zbus.mq.server.MqAdaptor;
import org.zbus.mq.server.MqServer;
import org.zbus.net.Sync;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.Sync.Ticket;
import org.zbus.net.core.IoBuffer;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageInvoker;

public class LocalBroker extends Session implements Broker {
	private static final Logger log = Logger.getLogger(LocalBroker.class);

	protected MqServer mqServer;
	protected MqAdaptor adaptor;
	protected final Sync<Message, Message> sync = new Sync<Message, Message>();
	protected int readTimeout = 3000;

	public LocalBroker(MqServer mqServer) throws IOException {
		super(null, null, null);
		this.mqServer = mqServer;
		if(!this.mqServer.isStarted()){
			this.mqServer.start();
		}
		
		this.adaptor = this.mqServer.getDefaultMqAdaptor();
		this.setStatus(SessionStatus.CONNECTED);  
	}

	@Override
	public String getLocalAddress() {
		return "LocalBroker-Local-" + id();
	}

	@Override
	public String getRemoteAddress() {
		return "LocalBroker-Remote-" + id();
	}

	@Override
	public void write(IoBuffer buf) throws IOException {
		throw new IllegalArgumentException("IoBuffer not support in LocalBroker");
	}

	@Override
	public void write(Object obj) throws IOException {
		if (!(obj instanceof Message)) {
			throw new IllegalArgumentException("Message type required");
		}
		Message msg = (Message) obj;
		Ticket<Message, Message> ticket = sync.removeTicket(msg.getId());
		if (ticket != null) {
			ticket.notifyResponse(msg);
			return;
		}

		log.debug("!!!!!!!!!!!!!!!!!!!!!!!!!!Drop,%s", msg);

	}

	@Override
	public Message invokeSync(Message req, int timeout) throws IOException, InterruptedException {
		Ticket<Message, Message> ticket = null;
		try {
			ticket = sync.createTicket(req, timeout);
			invokeAsync(req, null);

			if (!ticket.await(timeout, TimeUnit.MILLISECONDS)) {
				return null;
			}
			return ticket.response();
		} finally {
			if (ticket != null) {
				sync.removeTicket(ticket.getId());
			}
		}
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException {
		Ticket<Message, Message> ticket = null;
		if (callback != null) {
			ticket = sync.createTicket(req, readTimeout, callback);
		} else {
			if (req.getId() == null) {
				req.setId(Ticket.nextId());
			}
		}
		try {
			adaptor.onMessage(req, this);
		} catch (IOException e) {
			if (ticket != null) {
				sync.removeTicket(ticket.getId());
			}
			throw e;
		}
	}

	@Override
	public void close() throws IOException {
		this.setStatus(SessionStatus.CLOSED);
	}

	@Override
	public MessageInvoker getClient(BrokerHint hint) throws IOException {
		return this;
	}

	@Override
	public void closeClient(MessageInvoker client) throws IOException {

	}

}
