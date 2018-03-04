package io.zbus.net.http;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.net.EventLoop;
import io.zbus.net.Client; 

public class HttpClient extends Client<HttpMsg, HttpMsg>{  
	
	public HttpClient(String address, final EventLoop loop){   
		super(address, loop);
		codec(p->{ 
			p.add(new HttpRequestEncoder()); 
			p.add(new HttpResponseDecoder());  
			p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
			p.add(new HttpMsgCodec()); 
		});  
	}
}
 
