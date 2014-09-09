package org.zbus.remoting;

import org.zbus.remoting.ticket.ResultCallback;


 
public class MyClient { 
	
	public static void main(String[] args) throws Exception {
		
		RemotingClient client = new RemotingClient("127.0.0.1:80");
		
		Message req = new Message();
		req.setCommand("test");
		req.setBody("hello");
		
		client.invokeAsync(req, new ResultCallback() {
			@Override
			public void onCompleted(Message result) {
				System.out.println(result);
				
			}
		}); 
	} 
}
