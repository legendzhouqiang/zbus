package io.zbus.mq.server;

import java.io.IOException;

import io.zbus.kit.JsonKit;
import io.zbus.mq.Message;
import io.zbus.net.Session;

public class ReplyKit { 
	
	public static void reply(Message req, Message res, Session sess) throws IOException {
		res.setId(req.getId());
		res.setTopic(req.getTopic());  
		if(res.getStatus() == null){
			res.setStatus(200);
		}
		sess.write(res);
	}
	
	public static void reply200(Message msg, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setTopic(msg.getTopic());
		res.setStatus(200);
		res.setBody("" + System.currentTimeMillis());

		sess.write(res);
	}
	
	public static void replyJson(Message msg, Session sess, Object object) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setTopic(msg.getTopic());
		res.setStatus(200);
		res.setJsonBody(JsonKit.toJSONString(object)); 

		sess.write(res);
	}

	public static void reply404(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String topic = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(404);
		res.setTopic(topic);
		res.setBody(String.format("404: Topic(%s) Not Found", topic));

		sess.write(res);
	}
	
	public static void reply404(Message msg, Session sess, String hint) throws IOException {
		Message res = new Message();
		String topic = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(404);
		res.setTopic(topic);
		res.setBody(String.format("404: %s", hint));

		sess.write(res);
	}
	
	public static void reply502(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String mqName = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(502);
		res.setTopic(mqName);
		res.setBody(String.format("502: Service(%s) Down", mqName));

		sess.write(res);
	}


	public static void reply403(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String mqName = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(403);
		res.setTopic(mqName);
		res.setBody(String.format("403: Topic(%s) Forbbiden", mqName));

		sess.write(res);
	} 
	
	public static void reply400(Message msg, Session sess, String hint) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setStatus(400);
		res.setTopic(msg.getTopic());
		res.setBody(String.format("400: Bad Format, %s", hint));
		sess.write(res);
	}
	
	public static void reply500(Message msg, Session sess, Exception ex) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setStatus(500);
		res.setTopic(msg.getTopic());
		res.setBody(String.format("500: Exception Caught, %s",ex));
		sess.write(res);
	}  
}
