package org.zstacks.zbus.protocol;

import java.util.Map;

import org.zstacks.znet.Message;

import com.alibaba.fastjson.JSON;

public class Proto {
	public static final String Heartbeat = Message.HEARTBEAT; //心跳消息 
	public static final String Produce   = "produce";         //生产消息
	public static final String Consume   = "consume";         //消费消息  
	public static final String Request   = "request";         //请求等待应答消息 
	public static final String Admin     = "admin";           //管理类消息  
	//管理类命令二级子命令 
	public static final String AdminCreateMQ    = "create_mq";  
	public static final String AdminQueryMQ     = "query_mq";  
	
	//TrackServer命令
	public static final String TrackReport      = "track_report"; 
	public static final String TrackSub         = "track_sub";  
	public static final String TrackPub         = "track_pub"; 
	//5.2.0 added
	public static final String TrackQuery       = "track_query"; 
	
	
	public static Message buildSubCommandMessage(String cmd, String subCmd, Map<String, String> params){
    	Message msg = new Message();
    	msg.setCommand(cmd); 
    	msg.setSubCommand(subCmd); 
    	if(params != null){
    		msg.setJsonBody(JSON.toJSONBytes(params));
    	}
    	return msg;
    }
	 
}
