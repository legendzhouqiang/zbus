package io.zbus.mq;

import java.io.IOException;

import io.zbus.kit.JsonKit;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.Protocol.TrackerInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.EventDriver;
 

public class MqClient extends MessageClient{         
	private String token;     
	
	public MqClient(String serverAddress, final EventDriver driver){
		super(serverAddress, driver);
	} 
	
	public Message produce(Message msg, int timeout) throws IOException, InterruptedException{ 
		msg.setCommand(Protocol.PRODUCE);
		return invokeSync(msg, timeout);  
	} 
	
	public Message produce(Message msg) throws IOException, InterruptedException{ 
		return produce(msg, invokeTimeout);  
	} 
	
	public void produceAsync(Message msg, MessageCallback callback) throws IOException {
		msg.setCommand(Protocol.PRODUCE);
		invokeAsync(msg, callback);
	}
	
	public Message consume(String topic) throws IOException, InterruptedException{
		return consume(topic, null, null);
	}
	
	public Message consume(String topic, String group) throws IOException, InterruptedException {
		return consume(topic, group, null);
	}
	
	public Message consume(String topic, String group, Integer window) throws IOException, InterruptedException {
		Message msg = new Message();
		msg.setCommand(Protocol.CONSUME);
		msg.setTopic(topic); 
		msg.setConsumeGroup(group);  
		msg.setConsumeWindow(window); 
		
		Message res = invokeSync(msg, invokeTimeout);
		if (res == null) return res;
		
		res.setId(res.getOriginId());
		res.removeHeader(Protocol.ORIGIN_ID);
		if (res.getStatus() == 200){
			String originUrl = res.getOriginUrl();
			if(originUrl == null){
				originUrl = "/";
			} else {
				res.removeHeader(Protocol.ORIGIN_URL);
			}
			res.setUrl(originUrl); 
		}
		return res;
	}  
	
	public TrackerInfo queryTracker() throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.TRACKER); 
		  
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, TrackerInfo.class); 
	}
	
	public ServerInfo queryServer() throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.SERVER); 
		  
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, ServerInfo.class); 
	}
	
	 
	public TopicInfo queryTopic(String topic) throws IOException, InterruptedException {
		Message msg = new Message();
		msg.setCommand(Protocol.QUERY);
		msg.setTopic(topic); 
		 
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, TopicInfo.class); 
	} 
	
	public ConsumeGroupInfo queryGroup(String topic, String group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.QUERY);
		msg.setTopic(topic); 
		msg.setConsumeGroup(group);
		  
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, ConsumeGroupInfo.class); 
	}
	
	
	public TopicInfo declareTopic(String topic) throws IOException, InterruptedException{
		return declareTopic(topic, null);
	}
	
	public TopicInfo declareTopic(String topic, Integer topicMask) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.DECLARE);
		msg.setTopic(topic); 
		msg.setTopicMask(topicMask);
		 
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, TopicInfo.class); 
	}
	
	public ConsumeGroupInfo declareGroup(String topic, ConsumeGroup group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.DECLARE);
		msg.setTopic(topic);
		msg.setConsumeGroup(group.getGroupName());
		msg.setGroupStartCopy(group.getStartCopy());
		msg.setGroupFilter(group.getFilter());
		msg.setGroupStartMsgId(group.getStartMsgId());
		msg.setGroupStartOffset(group.getStartOffset()); 
		msg.setGroupStartTime(group.getStartTime());
		msg.setTopicMask(group.getMask());
		
		
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, ConsumeGroupInfo.class); 
	}
	
	
	public void removeTopic(String topic) throws IOException, InterruptedException{
		removeGroup(topic, null);
	}
	
	public void removeGroup(String topic, String group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.REMOVE);
		msg.setTopic(topic); 
		msg.setConsumeGroup(group);
		 
		Message res = invokeSync(msg, invokeTimeout);
		checkResult(res);
	}   
	 
	
	public void emptyTopic(String topic) throws IOException, InterruptedException{
		emptyGroup(topic, null);
	}    
	
	public void emptyGroup(String topic, String group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.EMPTY);
		msg.setTopic(topic); 
		msg.setConsumeGroup(group);
		 
		Message res = invokeSync(msg, invokeTimeout);
		checkResult(res); 
	}
	
	public void route(Message msg) throws IOException{
		msg.setCommand(Protocol.ROUTE);  
		msg.setAck(false); 
		//invoke message should be request typed, if not add origin_status header and change it to request type
		Integer status = msg.getStatus();
		if(status != null){
			msg.setOriginStatus(status); 
			msg.setStatus(null); //make it as request 
		} 
		
		invokeAsync(msg, (MessageCallback)null);  
	}  
	
	public Message invokeSync(Message msg, int timeout) throws IOException, InterruptedException {
		fillCommonHeaders(msg);
		return super.invokeSync(msg, timeout);
	}
	
	public Message invokeSync(Message msg) throws IOException, InterruptedException {
		return invokeSync(msg, invokeTimeout);
	}
	
	public void invokeAsync(Message msg, MessageCallback callback) throws IOException {
		fillCommonHeaders(msg);
	    super.invokeAsync(msg, callback);
	}
	
	private void fillCommonHeaders(Message msg){  
		if(msg.getToken() == null){
			msg.setToken(this.token);
		}
	}
	
	private void checkResult(Message msg){
		if(msg.getStatus() != 200 ){
			throw new MqException(msg.getBodyString());
		}
	}
	
	private <T> T parseResult(Message msg, Class<T> clazz){
		checkResult(msg); 
		
		try{
			return JsonKit.parseObject(msg.getBodyString(), clazz);
		} catch (Exception e) {
			throw new MqException(msg.getBodyString(), e);
		}
	} 

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	} 
}
