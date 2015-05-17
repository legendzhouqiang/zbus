package org.zstacks.zbus.server;

import java.io.IOException;

import org.zstacks.znet.Message;
import org.zstacks.znet.nio.Session;

public class ServerHelper { 
	
	public static void reply404(Message msg, Session sess) throws IOException{
    	Message res = new Message();
    	String mqName = msg.getMq();
		res.setMsgId(msg.getMsgId());  
		res.setStatus("404");
		res.setMqReply(sess.id()); //mark
		res.setBody(String.format("MQ(%s) Not Found", mqName));
		
		sess.write(res);
    }
   
    public static void reply403(Message msg, Session sess) throws IOException{
    	Message res = new Message();
    	String mqName = msg.getMq();
    	
    	res.setMsgId(msg.getMsgId()); 
    	res.setStatus("403");
    	res.setMqReply(sess.id()); //mark
    	res.setBody(String.format("MQ(%s) forbbiden, token(%s) mismatched", mqName, msg.getToken()));
    	
    	sess.write(res);
    }
    
    public static void reply200(String msgId, Session sess) throws IOException{
    	Message res = new Message();
    	res.setMsgId(msgId); 
    	res.setStatus("200");
    	res.setMqReply(sess.id()); //mark
    	res.setBody(""+System.currentTimeMillis()); 
    	 
    	sess.write(res);
    }
    
    
    public static void reply400(Message msg, Session sess) throws IOException{
    	Message res = new Message();
    	res.setMsgId(msg.getMsgId()); 
    	res.setStatus("400");
    	res.setMqReply(sess.id()); //mark
    	res.setBody(String.format("Bad format: %s", msg.getBodyString())); 
    	sess.write(res);
    }
    
}
