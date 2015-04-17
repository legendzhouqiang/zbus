package org.zbus.client;

import java.io.IOException;

import org.zbus.protocol.MessageMode;
import org.zbus.protocol.Proto;
import org.zbus.remoting.Message;
import org.zbus.remoting.ticket.ResultCallback;

/**
 * 生产者,
 * @author 洪磊明(rushmore)
 *
 */
public class Producer extends MqAdmin{    
	public Producer(Broker broker, String mq, MessageMode... mode) {
		super(broker, mq, mode);
	} 
	
	public Producer(MqConfig config){
		super(config);
	}

	
	public void send(Message msg, final ResultCallback callback)
			throws IOException {
		msg.setCommand(Proto.Produce);
		msg.setMq(this.mq);
		msg.setToken(this.accessToken);
		
		broker.invokeAsync(msg, callback);
	}
	
	public Message sendSync(Message msg, int timeout) throws IOException{
		msg.setCommand(Proto.Produce);
		msg.setMq(this.mq);
		msg.setToken(this.accessToken);
		
		return broker.invokeSync(msg, timeout);
	}
}
