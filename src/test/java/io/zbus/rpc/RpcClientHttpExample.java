package io.zbus.rpc;

import io.zbus.net.EventLoop;
import io.zbus.rpc.Request;
import io.zbus.rpc.RpcClient;

public class RpcClientHttpExample {

	public static void main(String[] args) throws Exception {
		EventLoop loop = new EventLoop();
		String address = "localhost";

		RpcClient client = new RpcClient(address, loop);

		client.onOpen = () -> {
			Request req = new Request();
			req.setModule("example");
			req.setMethod("plus");
			req.setParams(1, 2);
			
			client.invoke(req, resp -> {
				System.out.println(resp);
				client.close();
				loop.close();
			});
		};

		client.connect();
	}
}
