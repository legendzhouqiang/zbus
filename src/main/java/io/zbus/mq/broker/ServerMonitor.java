package io.zbus.mq.broker;

import java.io.Closeable;
import java.io.IOException;

import com.alibaba.fastjson.JSON;

import io.zbus.mq.Message;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.Client.ConnectedHandler;
import io.zbus.net.Client.DisconnectedHandler;
import io.zbus.net.Client.MsgHandler;
import io.zbus.net.EventDriver;
import io.zbus.net.Session;

public class ServerMonitor implements Closeable{
	private String serverAddress;
	private MessageClient client;
	private EventDriver eventDriver;
	
	private ServerChangeHandler onlineHandler;
	private ServerChangeHandler offlineHandler;
	private TopicChangeHandler topicChangeHandler;
	
	public ServerMonitor(String serverAddress, EventDriver eventDriver){
		this.serverAddress = serverAddress;
		this.eventDriver = eventDriver;
	}
	
	public void start() {
		if(this.client != null) return;
		
		this.client = new MessageClient(serverAddress, eventDriver);
		this.client.onConnected(new ConnectedHandler() {
			
			@Override
			public void onConnected() throws IOException { 
				if(onlineHandler != null){
					onlineHandler.onServerChange(serverAddress);
				} 
			}
		});
		
		this.client.onDisconnected(new DisconnectedHandler() {
			
			@Override
			public void onDisconnected() throws IOException { 
				if(offlineHandler != null){
					offlineHandler.onServerChange(serverAddress);
				} 
				client.ensureConnectedAsync();
			}
		});
		
		this.client.onMessage(new MsgHandler<Message>() { 
			@Override
			public void handle(Message msg, Session session) throws IOException { 
				String bodyString = msg.getBodyString();
				if(bodyString == null) return;
				try{
					TopicInfo topicInfo = JSON.parseObject(bodyString, TopicInfo.class);
					if(topicChangeHandler != null){
						topicChangeHandler.onTopicChange(topicInfo);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		});
		
		this.client.ensureConnectedAsync();
	}
	
	public ServerInfo queryServerInfo(){
		return new ServerInfo();//TODO
	}
	
	@Override
	public void close() throws IOException {
		this.client.onDisconnected(null);
		this.client.close();
	} 
	
	public ServerChangeHandler getOnlineHandler() {
		return onlineHandler;
	}

	public void setOnlineHandler(ServerChangeHandler onlineHandler) {
		this.onlineHandler = onlineHandler;
	}

	public ServerChangeHandler getOfflineHandler() {
		return offlineHandler;
	}

	public void setOfflineHandler(ServerChangeHandler offlineHandler) {
		this.offlineHandler = offlineHandler;
	}

	public TopicChangeHandler getTopicChangeHandler() {
		return topicChangeHandler;
	}

	public void setTopicChangeHandler(TopicChangeHandler topicChangeHandler) {
		this.topicChangeHandler = topicChangeHandler;
	}


	public static interface ServerChangeHandler{
		void onServerChange(String serverAddress); 
	} 
	public static interface TopicChangeHandler{
		void onTopicChange(TopicInfo topicInfo);
	} 
}
