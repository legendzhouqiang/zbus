package io.zbus.rpc;

public class RpcClientPerf {

	public static void main(String[] args) throws Exception {
		RpcClient rpc = new RpcClient("localhost");

		for (int i = 0; i < 1000000; i++) {
			Request req = new Request();
			req.setModule("example");
			req.setMethod("getOrder");
			Response res = rpc.invoke(req); // 同步调用
			if(i%10000==0) {
				System.out.println(i+ ":"+ res);
			}
		}
		rpc.close();
	}
}
