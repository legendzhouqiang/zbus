package org.zstacks.zbus.client;

import java.io.IOException;

import org.zstacks.zbus.protocol.MessageMode;
import org.zstacks.zbus.protocol.Proto;
import org.zstacks.znet.Message;
import org.zstacks.znet.ticket.ResultCallback;

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
