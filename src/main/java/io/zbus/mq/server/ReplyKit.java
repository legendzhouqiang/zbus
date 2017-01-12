/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zbus.mq.server;

import java.io.IOException;

import io.zbus.mq.Message;
import io.zbus.net.Session;

public class ReplyKit {

	public static void reply200WithBody(Message msg, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setMq(msg.getMq());
		res.setStatus(200);
		res.setBody(msg.getBody());

		sess.write(res);
	}
	
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
	
	public static void reply502(Message msg, Session sess) throws IOException {
		Message res = new Message();
		String mqName = msg.getMq();
		res.setId(msg.getId());
		res.setStatus(502);
		res.setMq(mqName);
		res.setBody(String.format("MQ(%s) Service Down", mqName));

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
	
	public static void reply500(Message msg, Exception ex, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setStatus(400);
		res.setMq(msg.getMq());
		res.setBody(String.format("Exception caught: %s",ex));
		sess.write(res);
	}
	
	public static void reply406(Message msg, Session sess) throws IOException {
		Message res = new Message();
		res.setId(msg.getId());
		res.setMq(msg.getMq());
		res.setStatus(406);
		res.setBody("" + System.currentTimeMillis());

		sess.write(res);
	}

}
