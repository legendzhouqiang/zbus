package io.zbus.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.net.ErrorHandler;
import io.zbus.net.EventLoop;
import io.zbus.net.MessageHandler;
import io.zbus.net.http.HttpClient;
import io.zbus.net.http.HttpMessage;

public class RpcClient extends HttpClient {
	static class RequestContext {
		Request request;
		MessageHandler<Response> onData;
		ErrorHandler onError;
	}

	private Map<String, RequestContext> callbackTable = new ConcurrentHashMap<>();

	public RpcClient(String address, EventLoop loop) {
		super(address, loop);

		onMessage = msg -> {
			Response response = JsonKit.parseObject(msg.getBodyString(), Response.class);
			RequestContext ctx = callbackTable.get(response.getId());
			if (ctx != null) {
				ctx.onData.handle(response);
			}
		};
	}

	public void invoke(Request request, MessageHandler<Response> dataHandler) {
		invoke(request, dataHandler, null);
	}

	public void invoke(Request request, MessageHandler<Response> dataHandler, ErrorHandler errorHandler) {
		if (request.getId() == null)
			request.setId(StrKit.uuid());
		RequestContext ctx = new RequestContext();
		ctx.request = request;
		ctx.onData = dataHandler;
		ctx.onError = errorHandler;

		callbackTable.put(request.getId(), ctx);

		String reqString = JsonKit.toJSONString(request);
		HttpMessage req = new HttpMessage();
		req.setBody(reqString);
		sendMessage(req);
	}
}
