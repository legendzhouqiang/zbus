package org.zbus.net.http;

import java.io.IOException;

import org.zbus.net.InvokingClient;
import org.zbus.net.core.Dispatcher;


public class MessageClient extends InvokingClient<Message, Message>{   
	public MessageClient(String host, int port, Dispatcher dispatcher){
		super(host, port, dispatcher);
		codec(new MessageCodec());
	} 
	
	public MessageClient(String address, Dispatcher dispatcher) {
		super(address, dispatcher);
		codec(new MessageCodec());
	}
	
	@Override
	protected void heartbeat() {
		if(this.hasConnected()){
			Message hbt = new Message();
			hbt.setCmd(Message.HEARTBEAT);
			try {
				this.send(hbt);
			} catch (IOException e) {  
				//ignore
			}
		}
	}
}
