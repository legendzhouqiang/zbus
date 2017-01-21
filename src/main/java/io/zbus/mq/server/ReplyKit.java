package io.zbus.mq.server;

import java.io.IOException;

import io.zbus.mq.Message;
import io.zbus.net.Session;

public class ReplyKit {

	public static void reply200WithBody(Message msg, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setTopic(msg.getTopic());
		res.setStatus(200);
		res.setBody(msg.getBody());

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

	public static void reply404(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String mqName = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(404);
		res.setTopic(mqName);
		res.setBody(String.format("MQ(%s) Not Found", mqName));

		sess.write(res);
	}
	
	public static void reply502(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String mqName = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(502);
		res.setTopic(mqName);
		res.setBody(String.format("MQ(%s) Service Down", mqName));

		sess.write(res);
	}


	public static void reply403(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String mqName = msg.getTopic();
		res.setId(msg.getId());
		res.setStatus(403);
		res.setTopic(mqName);
		res.setBody(String.format("MQ(%s) forbbiden", mqName));

		sess.write(res);
	}

	public static void reply400(Message msg, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setStatus(400);
		res.setTopic(msg.getTopic());
		res.setBody(String.format("Bad format: %s", msg.getBodyString()));
		sess.write(res);
	}
	
	public static void reply500(Message msg, Exception ex, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setStatus(400);
		res.setTopic(msg.getTopic());
		res.setBody(String.format("Exception caught: %s",ex));
		sess.write(res);
	}
	
	public static void reply406(Message msg, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setTopic(msg.getTopic());
		res.setStatus(406);
		res.setBody("" + System.currentTimeMillis());

		sess.write(res);
	}

}
