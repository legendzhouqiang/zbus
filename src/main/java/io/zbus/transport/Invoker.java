package io.zbus.transport;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.zbus.auth.DefaultSign;
import io.zbus.auth.RequestSign;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.mq.Protocol;

public interface Invoker {

	void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler);

	void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler, ErrorHandler errorHandler);

	Map<String, Object> invoke(Map<String, Object> req) throws IOException, InterruptedException;

	Map<String, Object> invoke(Map<String, Object> req, long timeout, TimeUnit timeUnit)
			throws IOException, InterruptedException;

	
	public static abstract class AbstractInvoker implements Invoker {
		private static final Logger logger = LoggerFactory.getLogger(AbstractInvoker.class);
		public String apiKey;
		public String secretKey;
		public boolean authEnabled = false;
		public RequestSign requestSign = new DefaultSign();

		protected Map<String, RequestContext> callbackTable = new ConcurrentHashMap<>(); // id->context

		public abstract void sendMessage(Map<String, Object> data);

		public void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler) {
			invoke(req, dataHandler, null);
		}

		public void invoke(Map<String, Object> req, DataHandler<Map<String, Object>> dataHandler,
				ErrorHandler errorHandler) {

			String id = (String) req.get("id");
			if (id == null) {
				id = StrKit.uuid();
				req.put("id", id);
			}
			if (authEnabled) {
				if (apiKey == null) {
					throw new IllegalStateException("apiKey not set");
				}
				if (secretKey == null) {
					throw new IllegalStateException("secretKey not set");
				}

				requestSign.sign(req, apiKey, secretKey);
			}

			RequestContext ctx = new RequestContext(req, dataHandler, errorHandler);
			callbackTable.put(id, ctx);

			sendMessage(req);
		}

		public Map<String, Object> invoke(Map<String, Object> req) throws IOException, InterruptedException {
			return invoke(req, 10, TimeUnit.SECONDS);
		}

		public Map<String, Object> invoke(Map<String, Object> req, long timeout, TimeUnit timeUnit)
				throws IOException, InterruptedException {
			CountDownLatch countDown = new CountDownLatch(1);
			AtomicReference<Map<String, Object>> res = new AtomicReference<Map<String, Object>>();
			long start = System.currentTimeMillis();
			invoke(req, data -> {
				res.set(data);
				countDown.countDown();
			});
			countDown.await(timeout, timeUnit);
			if (res.get() == null) {
				long end = System.currentTimeMillis();
				String msg = String.format("Timeout(Time=%dms, ID=%s): %s", (end - start), (String) req.get("id"),
						JsonKit.toJSONString(req));
				throw new IOException(msg);
			}
			return res.get();
		}

		protected boolean onResponse(Map<String, Object> response) throws Exception {
			String id = (String) response.get(Protocol.ID);
			if (id != null) {
				RequestContext ctx = callbackTable.remove(id);
				if (ctx != null) { // 1) Request-Response invocation
					Integer status = (Integer) response.get(Protocol.STATUS);
					if (status != null && status != 200) {
						if (ctx.onError != null) {
							ctx.onError.handle(new RuntimeException((String) response.get(Protocol.BODY)));
						} else {
							logger.error(JsonKit.toJSONString(response));
						}
					} else {
						if (ctx.onData != null) {
							ctx.onData.handle(response);
						} else {
							logger.warn("Missing handler for: " + response);
						}
					}
					return true;
				}
			}
			return false;
		};

		public static class RequestContext {
			public Map<String, Object> request;
			public DataHandler<Map<String, Object>> onData;
			public ErrorHandler onError;

			RequestContext(Map<String, Object> request, DataHandler<Map<String, Object>> onData, ErrorHandler onError) {
				this.request = request;
				this.onData = onData;
				this.onError = onError;
			}
		}
	}
}
