package org.zbus.mq.server;

import java.io.IOException;

import org.zbus.net.core.Session;
import org.zbus.net.http.Message;

public class ReplyKit {

	public static void reply200(Message msg, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setMq(msg.getMq());
		res.setStatus(200);
		res.setBody("" + System.currentTimeMillis());

		sess.write(res);
	}

	public static void reply404(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String mqName = msg.getMq();
		res.setId(msg.getId());
		res.setStatus(404);
		res.setMq(mqName);
		res.setBody(String.format("MQ(%s) Not Found", mqName));

		sess.write(res);
	}

	public static void reply403(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String mqName = msg.getMq();
		res.setId(msg.getId());
		res.setStatus(403);
		res.setMq(mqName);
		res.setBody(String.format("MQ(%s) forbbiden", mqName));

		sess.write(res);
	}

	public static void reply400(Message msg, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setStatus(400);
		res.setMq(msg.getMq());
		res.setBody(String.format("Bad format: %s", msg.getBodyString()));
		sess.write(res);
	}

}
