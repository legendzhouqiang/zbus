package io.zbus.rpc;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.zbus.auth.DefaultSign;
import io.zbus.auth.RequestSign;
import io.zbus.kit.JsonKit;
import io.zbus.kit.StrKit;
import io.zbus.transport.DataHandler;
import io.zbus.transport.ErrorHandler;
import io.zbus.transport.http.WebsocketClient;
import okhttp3.OkHttpClient; 

public class RpcClient extends WebsocketClient { 
	public String apiKey;
	public String secretKey;
	public boolean authEnabled = false;
	public RequestSign requestSign = new DefaultSign();
	
	private Map<String, RequestContext> callbackTable = new ConcurrentHashMap<>();

	public RpcClient(String address) {  
		this(address, new OkHttpClient());
	}  
	
	public RpcClient(String address, OkHttpClient httpClient) {
		super(address, httpClient);
		onText = msg -> {
			Response response = JsonKit.parseObject(msg, Response.class);
			RequestContext ctx = callbackTable.remove(response.getId());
			if (ctx != null) {
				ctx.onData.handle(response);
			}
		}; 
	}
	
	public void invoke(Request request, DataHandler<Response> dataHandler) {
		invoke(request, dataHandler, null);
	}

	public void invoke(Request request, DataHandler<Response> dataHandler, ErrorHandler errorHandler) {
		request.setId(StrKit.uuid());
		if(authEnabled) {
			if(apiKey == null) {
				throw new IllegalStateException("apiKey not set");
			}
			if(secretKey == null) {
				throw new IllegalStateException("secretKey not set");
			}
			
			requestSign.sign(request, apiKey, secretKey);
		}
		
		RequestContext ctx = new RequestContext();
		ctx.request = request;
		ctx.onData = dataHandler;
		ctx.onError = errorHandler;

		callbackTable.put(request.getId(), ctx);

		String reqString = JsonKit.toJSONString(request); 
		sendMessage(reqString);
	}
	
	public Response invoke(Request req) throws IOException, InterruptedException { 
		return invoke(req, 10000);
	}
	
	public Response invoke(Request req, long timeout) throws IOException, InterruptedException { 
		CountDownLatch countDown = new CountDownLatch(1);
		AtomicReference<Response> res = new AtomicReference<Response>();  
		long start = System.currentTimeMillis();
		invoke(req, resp->{
			res.set(resp);
			countDown.countDown();
		});
		countDown.await(timeout, TimeUnit.MILLISECONDS);
		if(res.get() == null){ 
			long end = System.currentTimeMillis();
			String msg = String.format("Timeout(Time=%dms, ID=%s): %s", (end-start), req.getId(), JsonKit.toJSONString(req)); 
			throw new IOException(msg);
		}
		return res.get();
	} 
	 
	public static <T> T parseResult(Response resp, Class<T> clazz) { 
		Object data = resp.getData();
		if(resp.getStatus() != 200){
			if(data instanceof RuntimeException){
				throw (RuntimeException)data;
			} else {
				throw new RpcException(data.toString());
			}
		} 
		try { 
			return (T) JsonKit.convert(data, clazz); 
		} catch (Exception e) { 
			throw new RpcException(e.getMessage(), e.getCause());
		}
	}  
	
	
	@SuppressWarnings("unchecked")
	public <T> T createProxy(Class<T> clazz, String module){  
		Constructor<RpcInvocationHandler> rpcInvokerCtor;
		try {
			rpcInvokerCtor = RpcInvocationHandler.class.getConstructor(new Class[] {RpcClient.class, String.class }); 
			RpcInvocationHandler rpcInvokerHandler = rpcInvokerCtor.newInstance(this, module); 
			Class<T>[] interfaces = new Class[] { clazz }; 
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			return (T) Proxy.newProxyInstance(classLoader, interfaces, rpcInvokerHandler);
		} catch (Exception e) { 
			throw new RpcException(e);
		}   
	}  
	
	
	public static class RpcInvocationHandler implements InvocationHandler {  
		private RpcClient rpc; 
		private String module;
		private static final Object REMOTE_METHOD_CALL = new Object();

		public RpcInvocationHandler(RpcClient rpc, String module) {
			this.rpc = rpc;
			this.module = module;
		}
		
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if(args == null){
				args = new Object[0];
			}
			Object value = handleLocalMethod(proxy, method, args);
			if (value != REMOTE_METHOD_CALL) return value; 
			 
			Request request = new Request();
			request.setModule(module);
			request.setMethod(method.getName());  
			request.setParams(args);
			
			Response resp = rpc.invoke(request);
			return parseResult(resp, method.getReturnType());
		}

		protected Object handleLocalMethod(Object proxy, Method method,
				Object[] args) throws Throwable {
			String methodName = method.getName();
			Class<?>[] params = method.getParameterTypes();

			if (methodName.equals("equals") && params.length == 1
					&& params[0].equals(Object.class)) {
				Object value0 = args[0];
				if (value0 == null || !Proxy.isProxyClass(value0.getClass()))
					return new Boolean(false);
				RpcInvocationHandler handler = (RpcInvocationHandler) Proxy.getInvocationHandler(value0);
				return new Boolean(this.rpc.equals(handler.rpc));
			} else if (methodName.equals("hashCode") && params.length == 0) {
				return new Integer(this.rpc.hashCode());
			} else if (methodName.equals("toString") && params.length == 0) {
				return "RpcInvocationHandler[" + this.rpc + "]";
			}
			return REMOTE_METHOD_CALL;
		} 
	}
	
	static class RequestContext {
		Request request;
		DataHandler<Response> onData;
		ErrorHandler onError;
	}
}
