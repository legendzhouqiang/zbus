package io.zbus.net.http;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion; 


public class HttpClientCodec extends MessageToMessageCodec<io.netty.handler.codec.http.HttpMessage, HttpMessage> { 
 
	@Override
	protected void encode(ChannelHandlerContext ctx, HttpMessage msg, List<Object> out) throws Exception {  
		FullHttpMessage httpMsg = null;
		if (msg.getStatus() == null) {// as request
			httpMsg = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(msg.getMethod()),
					msg.getUrl()); 
		} else {// as response
			httpMsg = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
					HttpResponseStatus.valueOf(Integer.valueOf(msg.getStatus())));
		}
		//content-type and encoding
		String contentType = msg.getHeader(HttpMessage.CONTENT_TYPE); 
		String encoding = msg.getHeader(HttpMessage.ENCODING);
		if(encoding != null){
			encoding = "utf-8";
			if(contentType == null) {
				contentType = "text/plain";
			}
			contentType += "; charset=" + encoding;
		}
		if(contentType != null){
			httpMsg.headers().set(HttpMessage.CONTENT_TYPE, contentType);
		}
		
		for (Entry<String, String> e : msg.getHeaders().entrySet()) {
			if(e.getKey().equalsIgnoreCase(HttpMessage.CONTENT_TYPE)) continue;
			if(e.getKey().equalsIgnoreCase(HttpMessage.ENCODING)) continue;
			
			httpMsg.headers().add(e.getKey().toLowerCase(), e.getValue());
		}
		if (msg.getBody() != null) {
			httpMsg.content().writeBytes(msg.getBody());
		}

		out.add(httpMsg);
	}

	private HttpMessage decodeHeaders(io.netty.handler.codec.http.HttpMessage httpMsg){
		HttpMessage msg = new HttpMessage();
		Iterator<Entry<String, String>> iter = httpMsg.headers().iteratorAsString();
		while (iter.hasNext()) {
			Entry<String, String> e = iter.next();
			if(e.getKey().equalsIgnoreCase(HttpMessage.CONTENT_TYPE)){ //encoding and type
				String[] typeInfo = httpContentType(e.getValue());
				msg.setHeader(HttpMessage.CONTENT_TYPE, typeInfo[0]); 
				if(msg.getHeader(HttpMessage.ENCODING) == null) {
					msg.setHeader(HttpMessage.ENCODING, typeInfo[1]);
				}
			} else {
				msg.setHeader(e.getKey().toLowerCase(), e.getValue());
			} 
		}  

		if (httpMsg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) httpMsg;
			msg.setMethod(req.method().name());
			msg.setUrl(req.uri());
		} else if (httpMsg instanceof HttpResponse) {
			HttpResponse resp = (HttpResponse) httpMsg;
			int status = resp.status().code();
			msg.setStatus(status);
		}
		return msg;
	} 
	
	@Override
	protected void decode(ChannelHandlerContext ctx, io.netty.handler.codec.http.HttpMessage httpMsg, List<Object> out) throws Exception {   
		HttpMessage msg = decodeHeaders(httpMsg);   
		
		//Body
		ByteBuf body = null;
		if (httpMsg instanceof FullHttpMessage) {
			FullHttpMessage fullReq = (FullHttpMessage) httpMsg;
			body = fullReq.content();
		} 
		
		if (body != null) { 
			int size = body.readableBytes();
			if (size > 0) {
				byte[] data = new byte[size];
				body.readBytes(data);
				msg.setBody(data);
			}
		}

		out.add(msg);
	} 
	 
	private static String[] httpContentType(String value){
		String type="text/plain", charset="utf-8";
		String[] bb = value.split(";");
		if(bb.length>0){
			type = bb[0].trim();
		}
		if(bb.length>1){
			String[] bb2 = bb[1].trim().split("=");
			if(bb2[0].trim().equalsIgnoreCase("charset")){
				charset = bb2[1].trim();
			}
		}
		return new String[]{type, charset};
	}  
}
