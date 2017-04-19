package io.zbus.mq;

import java.io.IOException;

import io.zbus.kit.JsonKit;
import io.zbus.mq.Protocol.ConsumeGroupInfo;
import io.zbus.mq.Protocol.ServerInfo;
import io.zbus.mq.Protocol.TopicInfo;
import io.zbus.mq.net.MessageClient;
import io.zbus.net.EventDriver;
 

public class MqClient extends MessageClient{        
	private String appid;
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
		return consume(topic, null);
	}
	
	public Message consume(String topic, ConsumeCtrl ctrl) throws IOException, InterruptedException {
		Message msg = new Message();
		msg.setCommand(Protocol.CONSUME);
		msg.setTopic(topic);
		
		if(ctrl != null){
			msg.setConsumeGroup(ctrl.getGroupName()); 
			msg.setConsumeFilterTag(ctrl.getFilterTag());
			msg.setConsumeWindow(ctrl.getWindow());
		} 
		
		Message res = invokeSync(msg, invokeTimeout);
		if (res == null) return res;
		
		res.setId(res.getOriginId());
		res.removeHeader(Protocol.ORIGIN_ID);
		if ("200".equals(res.getStatus())){
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
	
	public ServerInfo queryServer() throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.QUERY); 
		  
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
		Message msg = new Message();
		msg.setCommand(Protocol.DECLARE);
		msg.setTopic(topic); 
		 
		Message res = invokeSync(msg, invokeTimeout);
		return parseResult(res, TopicInfo.class); 
	}
	
	public ConsumeGroupInfo declareGroup(String topic, ConsumeGroup group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.DECLARE);
		msg.setTopic(topic);
		msg.setConsumeGroup(group.getGroupName());
		msg.setConsumeGroupCopyFrom(group.getGroupCopyFrom());
		msg.setConsumeFilterTag(group.getFilterTag());
		msg.setConsumeStartMsgId(group.getStartMsgId());
		msg.setConsumeStartOffset(group.getStartOffset()); 
		msg.setConsumeStartTime(group.getStartTime());
		
		
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
	
	
	public void pauseTopic(String topic) throws IOException, InterruptedException{
		pauseGroup(topic, null);
	}
	
	public void pauseGroup(String topic, String group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.PAUSE);
		msg.setTopic(topic); 
		msg.setConsumeGroup(group);
		 
		Message res = invokeSync(msg, invokeTimeout);
		checkResult(res);
	}
	
	
	public void resumeTopic(String topic) throws IOException, InterruptedException{
		resumeGroup(topic, null);
	}
	
	public void resumeGroup(String topic, String group) throws IOException, InterruptedException{
		Message msg = new Message();
		msg.setCommand(Protocol.RESUME);
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
		String status = msg.getStatus();
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
		if(msg.getAppid() == null){
			msg.setAppid(this.appid);
		}
		if(msg.getToken() == null){
			msg.setToken(this.token);
		}
	}
	
	private void checkResult(Message msg){
		if(!"200".equals(msg.getStatus())){
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

	public String getAppid() {
		return appid;
	}

	public void setAppid(String appid) {
		this.appid = appid;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	} 
}