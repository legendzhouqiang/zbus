package io.zbus.rpc.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.zbus.kit.HttpKit;
import io.zbus.kit.HttpKit.UrlInfo;
import io.zbus.kit.JsonKit;
import io.zbus.rpc.Protocol;
import io.zbus.rpc.RpcProcessor;
import io.zbus.transport.ServerAdaptor;
import io.zbus.transport.Session;
import io.zbus.transport.http.HttpMessage;

public class RpcServerAdaptor extends ServerAdaptor {
	protected final RpcProcessor processor; 

	public RpcServerAdaptor(RpcProcessor processor) {
		this.processor = processor;
	}

	@Override
	public void onMessage(Object msg, Session sess) throws IOException {
		HttpMessage reqMsg = null;
		Map<String, Object> request = null;
		boolean writeHttp = true;
		if (msg instanceof HttpMessage) {
			reqMsg = (HttpMessage) msg;
			request = handleUrlMessage(reqMsg);
			if (request == null) {
				request = JsonKit.parseObject(reqMsg.getBodyString());
			}
		} else if (msg instanceof byte[]) {
			request = JsonKit.parseObject((byte[]) msg);
			writeHttp = false;
		}
		
		Map<String, Object> response = processor.process(request); 
		Object body = response.get(Protocol.BODY);
		if (body != null && body instanceof HttpMessage) {
			HttpMessage res = (HttpMessage)body;
			if (writeHttp) {
				if (res.getStatus() == null) {
					res.setStatus(200);
				}
				sess.write(res);
				return;
			} else {
				response.put(Protocol.BODY, res.toString());
			}
		}

		byte[] data = JsonKit.toJSONBytes(response, "utf8");

		if (writeHttp) {
			HttpMessage resMsg = new HttpMessage();
			resMsg.setStatus(200);
			resMsg.setEncoding("utf8");
			resMsg.setHeader("content-type", "application/json");
			resMsg.setBody(data);
			sess.write(resMsg);
		} else {
			sess.write(data);
		}
	}

	protected Map<String, Object> handleUrlMessage(HttpMessage msg) {
		String url = msg.getUrl();
		if (url == null || "/".equals(url)) {
			return null;
		}
		if (msg.getBody() != null)
			return null;

		UrlInfo info = HttpKit.parseUrl(url);

		Map<String, Object> req = new HashMap<String, Object>();
		if (info.path.size() >= 1) {
			req.put(Protocol.MODULE, info.path.get(0));
		}
		if (info.path.size() >= 2) {
			req.put(Protocol.METHOD, info.path.get(1));
		}

		if (info.path.size() > 2) {
			Object[] params = new Object[info.path.size() - 2];
			req.put(Protocol.PARAMS, params);
			for (int i = 0; i < info.path.size() - 2; i++) {
				params[i] = info.path.get(2 + i);
			}
		}
		return req;
	}
}
