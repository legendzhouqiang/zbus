package org.zbus.examples.net;

import java.io.IOException;
import java.util.List;

import org.zbus.kit.ConfigKit;
import org.zbus.net.Client.MsgHandler;
import org.zbus.net.CodecInitializer;
import org.zbus.net.IoDriver;
import org.zbus.net.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageToHttpWsCodec;
import org.zbus.net.tcp.TcpClient;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

public class TcpClientExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		String address = ConfigKit.option(args, "-h", "127.0.0.1:8080");
		
		IoDriver driver = new IoDriver();
		
		TcpClient<Message, Message> client = new TcpClient<Message, Message>(address, driver);

		client.codec(new CodecInitializer() {
			@Override
			public void initPipeline(List<ChannelHandler> p) { 
				p.add(new HttpRequestEncoder()); 
				p.add(new HttpResponseDecoder());  
				
				p.add(new HttpObjectAggregator(1024 * 1024 * 32)); 
				p.add(new MessageToHttpWsCodec());
			}
		});

		client.onMessage(new MsgHandler<Message>() {
			@Override
			public void handle(Message msg, Session session) throws IOException {
				System.err.println(msg);
			}
		});
		
		Message req = new Message();
		//req.setStatus(200);
		req.setBody("test ok");
		client.sendMessage(req); 
		
		client.sendMessage(req); 

		System.out.println("---done---");
	}
}
