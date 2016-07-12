package org.zbus.examples.net;

import java.util.List;

import org.zbus.kit.ConfigKit;
import org.zbus.net.CodecInitializer;
import org.zbus.net.EventDriver;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageToHttpWsCodec;
import org.zbus.net.tcp.TcpClient;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

public class NetClientExample { 
 
	public static void main(String[] args) throws Exception {
		String address = ConfigKit.option(args, "-h", "127.0.0.1:8080"); 
		EventDriver driver = new EventDriver();
		TcpClient<Message, Message> client = new TcpClient<Message, Message>(address, driver);
		
		client.codec(new CodecInitializer() { 
			@Override
			public void initPipeline(List<ChannelHandler> p) {
				p.add(new HttpClientCodec());
				p.add(new HttpObjectAggregator(1024 * 1024 * 32));
				p.add(new MessageToHttpWsCodec());
			}
		});
		
		
		Message msg = new Message();  
		msg.setBody("hello world");
		
		client.invokeSync(msg); 
		
		client.close();
		driver.close();
		System.out.println("---done---");
	}
}
