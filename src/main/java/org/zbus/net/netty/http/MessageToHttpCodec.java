package org.zbus.net.netty.http;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.zbus.net.http.Message;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class MessageToHttpCodec extends MessageToMessageCodec<HttpMessage, Message> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, List<Object> out) throws Exception {
		FullHttpMessage httpMsg = null;
		
		if(msg.getStatus() == null){//as request
			httpMsg = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, 
					HttpMethod.valueOf(msg.getMethod()), msg.getUrl());
		} else {//as response
			httpMsg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
					HttpResponseStatus.valueOf(Integer.valueOf(msg.getStatus())));
		}
		
		for(Entry<String, String> e : msg.getHead().entrySet()){
			httpMsg.headers().add(e.getKey().toLowerCase(), e.getValue());
		}
		if(msg.getBody() != null){
			httpMsg.content().writeBytes(msg.getBody());
		}
		
		out.add(httpMsg);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, HttpMessage httpMsg, List<Object> out) throws Exception {
		Message msg = new Message(); 
		Iterator<Entry<String, String>> iter = httpMsg.headers().iterator();
		while(iter.hasNext()){
			Entry<String, String> e = iter.next();
			msg.setHead(e.getKey().toLowerCase(), e.getValue());
		} 
		
		if(httpMsg instanceof HttpRequest){
			HttpRequest req = (HttpRequest)httpMsg;  
			msg.setMethod(req.getMethod().name());
			msg.setUrl(req.getUri());
		} else if(httpMsg instanceof HttpResponse){
			HttpResponse resp = (HttpResponse)httpMsg; 
			int status = resp.getStatus().code();
			msg.setStatus(status);
		} 
		
		if(httpMsg instanceof FullHttpMessage){
			FullHttpMessage fullReq = (FullHttpMessage)httpMsg;
			int size = fullReq.content().readableBytes();
			if(size > 0){
				byte[] data = new byte[size];
				fullReq.content().readBytes(data);
				msg.setBody(data);
			}
		} 
		
		out.add(msg); 
	}

}
