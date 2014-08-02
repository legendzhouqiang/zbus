package org.zbus.sendreply;

import org.remoting.Message;
import org.remoting.RemotingClient;
import org.zbus.client.Consumer;
import org.zbus.client.rpc.json.JsonHelper;

import com.alibaba.fastjson.JSONObject;

public class JsonRpcRecver {

	public static void main(String[] args) throws Exception {  
		final RemotingClient client = new RemotingClient("127.0.0.1", 15555);
		Consumer c = new Consumer(client, "MyJsonRpcReply");
		while(true){
			Message msg = c.recv(10000);
			if(msg == null) continue;
			JSONObject res = JsonHelper.unpackReplyJson(msg);
			System.out.println(res);
		}
	}
}
