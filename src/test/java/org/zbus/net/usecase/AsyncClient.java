package org.zbus.net.usecase;


import java.io.IOException;

import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class AsyncClient {

	public static void main(String[] args) throws Exception { 
		final Dispatcher dispatcher = new Dispatcher();

		final MessageClient client = new MessageClient("127.0.0.1:80", dispatcher);
	
		Message msg = new Message();
		msg.setCmd("hello");
		msg.setBody("hello");
		//异步请求
		client.invokeAsync(msg, new ResultCallback<Message>() {
			
			@Override
			public void onReturn(Message result) {
				System.out.println(result);
				try {
					client.close();
					dispatcher.close();
				} catch (IOException e) {
					//ignore
				} 
			}
		});
	}

}
