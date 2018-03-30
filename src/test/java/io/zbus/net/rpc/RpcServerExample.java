package io.zbus.net.rpc;

import io.zbus.net.rpc.biz.InterfaceExampleImpl;
import io.zbus.rpc.http.ServiceBootstrap;

public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		ServiceBootstrap b = new ServiceBootstrap();

		b.processor().setAuthFilter((req, resp) -> {
			if("map".equals(req.getMethod())) { 
				resp.setStatus(403);
				resp.setData("Access Denied: map");
				return false; 
			}
			return true;
		});

		b.processor().setBeforeFilter((req, resp) -> { 
			return true;
		});

		b.stackTrace(false)
		 .addModule("example", InterfaceExampleImpl.class) 
		 .port(80)
		 .start();
	}
}
