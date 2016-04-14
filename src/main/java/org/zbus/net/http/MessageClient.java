package org.zbus.net.http;

import java.io.IOException;
import java.util.List;

import org.zbus.net.Client;
import org.zbus.net.CodecInitializer;
import org.zbus.net.EventDriver;
import org.zbus.net.Session;
import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.net.netty.NettyClient;
import org.zbus.net.netty.http.MessageToHttpCodec;
import org.zbus.net.simple.DefaultClient;
import org.zbus.net.simple.http.MessageCodec;

import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

public class MessageClient implements Client<Message, Message>, MessageInvoker{
	private Client<Message, Message> support;
	public MessageClient(String address, EventDriver driver){
		if(driver.isNettyEnabled()){
			support = new NettyClient<Message, Message>(address, driver);
			codec(new CodecInitializer() {
				@Override
				public void initPipeline(List<Object> p) {
					p.add(new HttpClientCodec());
					p.add(new HttpObjectAggregator(1024*1024*10)); //maximum of 10M
					p.add(new MessageToHttpCodec());
				}
			});
		} else {
			support = new DefaultClient<Message, Message>(address, driver);
			codec(new CodecInitializer() { 
				@Override
				public void initPipeline(List<Object> p) {
					p.add(new MessageCodec());
				}
			});
		}
		startHeartbeat(30000);//sending heartbeat every 5 minute
	}
	
	@Override
	public void heartbeat() {
		if(this.hasConnected()){
			Message hbt = new Message();
			hbt.setCmd(Message.HEARTBEAT);
			try {
				this.invokeAsync(hbt, null);
			} catch (IOException e) {  
				//ignore
			}
		}
	}
	
	@Override
	public Message invokeSync(Message req, int timeout) throws IOException, InterruptedException {
		return support.invokeSync(req, timeout);
	}

	@Override
	public Message invokeSync(Message req) throws IOException, InterruptedException {
		return support.invokeSync(req);
	}

	@Override
	public void invokeAsync(Message req, ResultCallback<Message> callback) throws IOException {
		support.invokeAsync(req, callback);
	}

	@Override
	public void onSessionAccepted(Session sess) throws IOException {
		support.onSessionAccepted(sess);
	}

	@Override
	public void onSessionRegistered(Session sess) throws IOException {
		support.onSessionRegistered(sess);
	}

	@Override
	public void onSessionConnected(Session sess) throws IOException {
		support.onSessionConnected(sess);
	}

	@Override
	public void onSessionToDestroy(Session sess) throws IOException {
		support.onSessionToDestroy(sess);
	}

	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		support.onMessage(msg, sess);
	}

	@Override
	public void onException(Throwable e, Session sess) throws Exception {
		support.onException(e, sess);
	}

	@Override
	public void close() throws IOException {
		support.close();
	}

	@Override
	public void startHeartbeat(int heartbeatInterval) {
		support.startHeartbeat(heartbeatInterval);
	}

	@Override
	public void codec(CodecInitializer codecInitializer) {
		support.codec(codecInitializer);
	}

	@Override
	public boolean hasConnected() {
		return support.hasConnected();
	}

	@Override
	public void connectAsync() throws IOException {
		support.connectAsync();
	}

	@Override
	public void ensureConnected() throws IOException, InterruptedException {
		support.ensureConnected();
	}
	
	@Override
	public void ensureConnectedAsync() {
		support.ensureConnectedAsync();
	}

	@Override
	public void sendMessage(Message req) throws IOException, InterruptedException {
		support.sendMessage(req);
	}

	@Override
	public void onMessage(MsgHandler<Message> msgHandler) {
		support.onMessage(msgHandler);
	}

	@Override
	public void onError(ErrorHandler errorHandler) {
		support.onError(errorHandler);
	}

	@Override
	public void onConnected(ConnectedHandler connectedHandler) {
		support.onConnected(connectedHandler);
	}

	@Override
	public void onDisconnected(DisconnectedHandler disconnectedHandler) {
		support.onDisconnected(disconnectedHandler);
	}

	@Override
	public <V> V attr(String key) {
		return support.attr(key);
	}

	@Override
	public <V> void attr(String key, V value) {
		support.attr(key, value);
	}
	
	@Override
	public String toString() {
		return "MessageClient"+support.toString();
	}
	
}
 
