package org.zbus.client.rpc;

import org.zbus.remoting.Message;

public interface Codec {
	Message  encodeRequest(Request request);
	Message  encodeResponse(Response response);
	Request  decodeRequest(Message msg);
	Response decodeResponse(Message msg);
	
	Object normalize(Object param, Class<?> targetType) throws ClassNotFoundException;
}
