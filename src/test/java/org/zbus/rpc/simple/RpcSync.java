package org.zbus.rpc.simple;

import org.remoting.Message;
import org.remoting.RemotingClient;
import org.zbus.client.rpc.Rpc;

public class RpcSync {

	public static void main(String[] args) throws Exception {  
		final RemotingClient client = new RemotingClient("127.0.0.1", 15555);
		
		Rpc rpc = new Rpc(client, "MyRpc");  
		
		for(int i=0;i<10;i++){
			Message req = new Message(); 
			req.setBody("hello from client "+i); 
			
			Message reply = rpc.invokeSync(req, 10000); 
			System.out.println(reply);
		}
		
		System.out.println("--done--");
	}
}
