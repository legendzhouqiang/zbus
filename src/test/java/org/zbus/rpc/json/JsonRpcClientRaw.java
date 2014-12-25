package org.zbus.rpc.json;

import java.util.HashMap;
import java.util.Map;

import org.zbus.client.rpc.json.JsonRpc;
import org.zbus.remoting.RemotingClient;


public class JsonRpcClientRaw {
	
	public static void main(String[] args) throws Throwable { 
		RemotingClient client = new RemotingClient("127.0.0.1:15555");
		
		
		JsonRpc rpc = new JsonRpc(client, "KCXP");
		String funcId = "testFunc2413336";
		Map<String, String> params = new HashMap<String, String>();
		params.put("key1", "val1");
		params.put("key2", "val2");
		
		rpc.invokeSync(funcId, params);
	}
}
