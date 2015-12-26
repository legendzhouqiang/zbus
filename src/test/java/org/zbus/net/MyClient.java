package org.zbus.net;

import org.zbus.net.Sync.ResultCallback;
import org.zbus.net.core.Dispatcher;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

public class MyClient {

	public static void main(String[] args) throws Exception {
		final Dispatcher dispatcher = new Dispatcher();

		final MessageClient client = new MessageClient("127.0.0.1:8080", dispatcher);

		Message msg = new Message();
		msg.setCmd("hello");

		msg.setBody("hello world");
		client.invokeAsync(msg, new ResultCallback<Message>() {

			@Override
			public void onReturn(Message result) {
				System.out.println(result);
			}
		});
	}
}
