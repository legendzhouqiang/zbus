package io.zbus.rpc;

import java.util.Map;

public class RpcClientPerf {

	public static void main(String[] args) throws Exception {
		RpcClient rpc = new RpcClient("localhost:8080");

		for (int i = 0; i < 1000000; i++) {
			Request req = new Request();
			req.setModule("example");
			req.setMethod("getOrder");
			Map<String, Object> res = rpc.invoke(req); // 同步调用
			if(i%10000==0) {
				System.out.println(i+ ":"+ res);
			}
		}
		rpc.close();
	}
}
