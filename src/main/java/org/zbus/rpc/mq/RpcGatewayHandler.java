package org.zbus.rpc.mq;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.zbus.kit.log.Logger;
import org.zbus.mq.Protocol;
import org.zbus.net.core.Session;
import org.zbus.net.http.Message;
import org.zbus.net.http.Message.MessageHandler;
import org.zbus.rpc.JsonRpcCodec;
import org.zbus.rpc.RpcCodec;
import org.zbus.rpc.RpcCodec.Request;
import org.zbus.rpc.RpcCodec.Response;

public abstract class RpcGatewayHandler implements MessageHandler {
	private static final Logger log = Logger.getLogger(RpcGatewayHandler.class);
	
	protected RpcCodec codec = new JsonRpcCodec(); 
	
	protected void onResponse(Object result, Throwable error, final Message rawMsg, Session reqSession) {
		Response resp = new Response();
		int status = 200;
		resp.setResult(result);
		resp.setEncoding(rawMsg.getEncoding());

		if (error != null) {
			if (error instanceof InvocationTargetException) {
				InvocationTargetException te = (InvocationTargetException) error;
				resp.setError(te.getTargetException());
			}
			resp.setError(error);
			status = 500;
		}
		
		Message res = codec.encodeResponse(resp);
		res.setResponseStatus(status);
		res.setCmd(Protocol.Route);
		res.setId(rawMsg.getId()); 
		res.setRecver(rawMsg.getSender());
		res.setAck(false); // make sure no reply message required
		
		try {
			reqSession.write(result);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}

	protected abstract void onRequest(Request req, final Message rawMsg, final Session reqSession);

	@Override
	public void handle(Message msg, Session sess) throws IOException {
		Request req = null;
		try {
			req = codec.decodeRequest(msg); 
		} catch (Exception e) { 
			onResponse(null, e, msg, sess);
			return;
		} 
		
		onRequest(req, msg, sess);  
	}
}