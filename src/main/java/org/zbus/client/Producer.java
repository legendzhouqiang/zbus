package org.zbus.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.zbus.common.MessageMode;
import org.zbus.common.Proto;
import org.zbus.remoting.Message;
import org.zbus.remoting.ticket.ResultCallback;

public class Producer {     
	private final Broker broker; 
	private final String mq;
	private String accessToken = "";

	public Producer(Broker broker, String mq) {
		this.broker = broker;
		this.mq = mq; 
	} 

	public boolean createMQ(String mq, String registerToken,
			MessageMode... mode) throws IOException {
		if (registerToken == null) {
			registerToken = "";
		}
		int modeValue = 0;
		if (mode.length == 0) {
			modeValue = MessageMode.intValue(MessageMode.MQ);
		} else {
			modeValue = MessageMode.intValue(mode);
		}

		Map<String, String> params = new HashMap<String, String>();
		params.put("mq_name", mq);
		params.put("access_token", accessToken);
		params.put("mq_mode", "" + modeValue);
		Message req = Proto.buildAdminMessage(registerToken, Proto.CreateMQ,
				params);
		 
		Message res = broker.produceMessage(req, 3000);
		if (res == null) return false;
		return res.isStatus200();
		
	}
	
	public void send(Message msg, final ResultCallback callback)
			throws IOException {
		msg.setCommand(Proto.Produce);
		msg.setMq(this.mq);
		msg.setToken(this.accessToken);
		
		broker.produceMessage(msg, callback);
	}
	

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
}
