package io.zbus.net.rpc;

import io.zbus.net.rpc.biz.InterfaceExampleImpl;
import io.zbus.rpc.http.ServiceBootstrap;

public class RpcServerExample {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		ServiceBootstrap b = new ServiceBootstrap();

		b.setAuthFilter((req, resp) -> {
			if("map".equals(req.getMethod())) { 
				resp.setStatus(403);
				resp.setData("Access Denied");
				return false; 
			}
			return true;
		});

		b.setBeforeFilter((req, resp) -> { 
			return true;
		});

		b.setStackTrace(false);
		b.addModule("example", InterfaceExampleImpl.class);
		b.setPort(80);
		b.start();
	}
}
