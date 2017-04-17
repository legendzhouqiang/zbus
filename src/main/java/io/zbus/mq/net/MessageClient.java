package io.zbus.mq.net;

import java.io.IOException;
import java.util.List;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.mq.Message;
import io.zbus.mq.MessageCallback;
import io.zbus.mq.MessageInvoker;
import io.zbus.net.CodecInitializer;
import io.zbus.net.EventDriver;
import io.zbus.net.ResultCallback;
import io.zbus.net.tcp.TcpClient;

public class MessageClient extends TcpClient<Message, Message> implements MessageInvoker{
	
	public MessageClient(String address, final EventDriver driver){
		super(address, driver, MessageIdentifier.INSTANCE, MessageIdentifier.INSTANCE);
		
		codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				p.add(new HttpObjectAggregator(driver.getPackageSizeLimit()));
				p.add(new MessageToHttpWsCodec());
			}
		});
		
		startHeartbeat(120);//sending heartbeat every 2 minutes
	}
	
	@Override
	public void heartbeat() {
		if(this.hasConnected()){
			Message hbt = new Message();
			hbt.setCommand(Message.HEARTBEAT);
			try {
				this.invokeAsync(hbt, (MessageCallback)null);
			} catch (IOException e) {  
				//ignore
			}
		}
	}  

	@Override
	public void invokeAsync(Message req, final MessageCallback callback) throws IOException {
		if(callback == null){
			super.invokeAsync(req, null);
		} else {
			super.invokeAsync(req, new ResultCallback<Message>() { 
				@Override
				public void onReturn(Message result) {
					callback.onReturn(result); 
				}
			});
		}
	} 
	
	public void invokeAsync(Message req) throws IOException {
		invokeAsync(req, (MessageCallback)null);
	}
}
 
