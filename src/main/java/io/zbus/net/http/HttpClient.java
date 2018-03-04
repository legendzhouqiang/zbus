package io.zbus.net.http;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.zbus.net.Client;
import io.zbus.net.ErrorHandler;
import io.zbus.net.EventLoop;
import io.zbus.net.MessageHandler;

public class HttpClient extends Client<HttpMsg, HttpMsg> {

	public HttpClient(String address, final EventLoop loop) {
		super(address, loop);
		codec(p -> {
			p.add(new HttpRequestEncoder());
			p.add(new HttpResponseDecoder());
			p.add(new HttpObjectAggregator(loop.getPackageSizeLimit()));
			p.add(new HttpMsgCodec());
		});
		
		onClose = null;  //Disable auto reconnect 
		onError = null;
	}
	
	public HttpMsg request(HttpMsg req) throws IOException, InterruptedException {
		return request(req, 3000);
	}

	public HttpMsg request(HttpMsg req, long timeout) throws IOException, InterruptedException {
		CountDownLatch countDown = new CountDownLatch(1);   
		AtomicReference<HttpMsg> res = new AtomicReference<HttpMsg>();
		onMessage = resp->{
			res.set(resp);
			countDown.countDown();
		};
		sendMessage(req);
		countDown.await(timeout, TimeUnit.MILLISECONDS);
		
		return res.get();
	}
	
	public void requestAsync(HttpMsg request,
			final MessageHandler<HttpMsg> messageHandler,
			final ErrorHandler errorHandler){
		onMessage = messageHandler;
		onError = errorHandler;
		
		sendMessage(request);
	}
}
