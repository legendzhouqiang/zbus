package org.zbus.common;

import java.util.Map;

import org.remoting.Message;

public class Proto {
	//生产消费者模式
	public static final String Produce   = "produce";     //生产消息
	public static final String Consume   = "consume";     //消费消息 
	
	//请求回复模式: 1个生产消费队列 + n个临时回复队列
	public static final String Request   = "request";     //请求等待应答消息 
	
	
	//心跳
	public static final String Heartbeat = "heartbeat"; //心跳消息
	
	//管理类
	public static final String Admin = "admin";      //管理类消息
	public static final String CreateMQ = "create_mq";      
	
	//TrackServer通讯
	public static final String TrackReport = "track_report"; 
	public static final String TrackSub    = "track_sub";  
	public static final String TrackPub    = "track_pub"; 
	
	public static Message buildAdminMessage(String registerToken, String cmd, Map<String, String> params){
    	Message msg = new Message();
    	msg.setCommand(Admin); 
    	msg.setToken(registerToken);
    	
    	msg.setHead("cmd", cmd); 
    	for(Map.Entry<String, String> e : params.entrySet()){
    		msg.setHead(e.getKey(), e.getValue());
    	}
    	return msg;
    }
	 
}
