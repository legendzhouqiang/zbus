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
package org.zbus.unittest.net.http.websocket;

import java.io.IOException;

import org.zbus.net.Server;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.SelectorGroup;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageCodec;

public class WebsocketAdaptor extends IoAdaptor {
	public WebsocketAdaptor() {
		codec(new MessageCodec());
	}

	public void onMessage(Object obj, Session sess) throws IOException {
		Message msg = (Message) obj;
		System.out.println(msg);
		
		

	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		SelectorGroup group = new SelectorGroup();
		Server server = new Server(group);
		server.start(8080, new WebsocketAdaptor());
	}
}
