package io.zbus.rpc.http;

import java.io.IOException;

import com.alibaba.fastjson.JSON;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
import io.zbus.net.ServerAdaptor;
import io.zbus.net.Session;
import io.zbus.net.http.HttpMessage;
import io.zbus.rpc.Request;
import io.zbus.rpc.Response;
import io.zbus.rpc.RpcProcessor;

public class HttpRpcServerAdaptor extends ServerAdaptor {
	protected final RpcProcessor processor; 

	public HttpRpcServerAdaptor(RpcProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		HttpMessage reqMsg = null;
		Request request = null;
		boolean writeHttp = true;
		if (msg instanceof HttpMessage) {
			reqMsg = (HttpMessage) msg;
			request = handleUrlMessage(reqMsg);
			if (request == null) {
				request = JSON.parseObject(reqMsg.getBodyString(), Request.class);
			}
		} else if (msg instanceof byte[]) {
			request = JSON.parseObject((byte[]) msg, Request.class);
			writeHttp = false;
		}
		
		Response response = processor.process(request); 
		
		if (response.getResult() != null && response.getResult() instanceof HttpMessage) {
			HttpMessage res = (HttpMessage) response.getResult();
			if (writeHttp) {
				if (res.getStatus() == null) {
					res.setStatus(200);
				}
				sess.write(res);
				return;
			} else {
				response.setResult(res.toString());
			}
		}

		byte[] data = JsonKit.toJSONBytes(response, "utf8");

		if (writeHttp) {
			HttpMessage resMsg = new HttpMessage();
			resMsg.setStatus(200);
			resMsg.setEncoding("utf8");
			resMsg.setBody(data);
			sess.write(resMsg);
		} else {
			sess.write(data);
		}
	}

	protected Request handleUrlMessage(HttpMessage msg) {
		String url = msg.getUrl();
		if (url == null || "/".equals(url)) {
			return null;
		}
		if (msg.getBody() != null)
			return null;

		UrlInfo info = HttpKit.parseUrl(url);

		Request req = new Request();
		if (info.path.size() >= 1) {
			req.setModule(info.path.get(0));
		}
		if (info.path.size() >= 2) {
			req.setMethod(info.path.get(1));
		}

		if (info.path.size() > 2) {
			Object[] params = new Object[info.path.size() - 2];
			req.setParams(params);
			for (int i = 0; i < info.path.size() - 2; i++) {
				params[i] = info.path.get(2 + i);
			}
		}
		return req;
	}
}
