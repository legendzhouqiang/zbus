package org.zbus.sendreply;

import org.remoting.Message;
import org.remoting.RemotingClient;
import org.remoting.ticket.ResultCallback;
import org.zbus.client.Producer;
import org.zbus.client.rpc.json.JsonHelper;
import org.zbus.client.rpc.json.JsonRequest;

public class JsonRpcSender {

	public static void main(String[] args) throws Exception {  
		final RemotingClient client = new RemotingClient("127.0.0.1", 15555);
		Producer p = new Producer(client, "MyJsonRpc"); //MQ=MyJsonRpc
		
		for(int i=0;i<100;i++){
			JsonRequest req = new JsonRequest();
			req.setModule("ServiceInterface");
			req.setMethod("echo");
			req.setParamTypes(new Class[]{String.class});
			req.setParams(new Object[]{"hello"}); 
		
			
			Message msg = JsonHelper.packJsonRequest(req);
			msg.setMqReply("MyJsonRpcReply");
			p.send(msg, new ResultCallback() {
				@Override
				public void onCompleted(Message result) {
					System.out.println(result);
				}
			});
		}
		
		System.out.println("--done--");
	}
}
