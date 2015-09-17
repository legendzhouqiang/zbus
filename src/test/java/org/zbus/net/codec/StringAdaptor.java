package org.zbus.net.codec;

import java.io.IOException;

import org.zbus.net.Server;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.core.IoAdaptor;
import org.zbus.net.core.Session;

public class StringAdaptor extends IoAdaptor{   
	public StringAdaptor(){
		codec(new StringCodec());
	}  
    public void onMessage(Object obj, Session sess) throws IOException {  
    	System.out.println("recv: " + obj);
    	sess.write(obj);
    }   
    @SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {  
		Dispatcher dispatcher = new Dispatcher(); //事件分发器 
		Server server = new Server(dispatcher);   //创建一个Server
		//指定端口侦听处理逻辑，可以侦听多个端口，每个端口相同处理IO逻辑或者不同IO逻辑
		server.registerAdaptor(8080, new StringAdaptor()); 
    	server.start();
	} 
}

