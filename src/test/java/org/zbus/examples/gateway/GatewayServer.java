package org.zbus.examples.gateway;

import java.io.IOException;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.Consumer;
import org.zbus.mq.MqConfig;
import org.zbus.mq.Protocol;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;
import org.zbus.net.http.Message.MessageHandler;

public class GatewayServer { 
	
	public static class GatewayMessageHandler implements MessageHandler{
		//TargetServer messaging 
		SelectorGroup group = new SelectorGroup();
		MessageClient targetClient;
		public GatewayMessageHandler() throws IOException{ 
			targetClient = new MessageClient("127.0.0.1:8080", group);
		}
		
		@Override
		public void handle(Message msg, Session sess) throws IOException {
			//msgId should be maintained
			final Session zbusSess = sess;
			final String msgId = msg.getId();
			final String sender = msg.getSender(); 
			targetClient.invokeAsync(msg, new ResultCallback<Message>() {

				@Override
				public void onReturn(Message result) { 
					result.setCmd(Protocol.Route);
					result.setRecver(sender);
					result.setId(msgId);
					result.setAck(false); //make sure no reply message required
					try {
						zbusSess.write(result);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			
		}
	}
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception { 
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker();
		
		MqConfig mqConfig = new MqConfig();
		mqConfig.setBroker(broker);
		mqConfig.setMq("Gateway");
		
		Consumer consumer = new Consumer(mqConfig);
		MessageHandler handler = new GatewayMessageHandler();
		consumer.start(handler);
		
	}

}
