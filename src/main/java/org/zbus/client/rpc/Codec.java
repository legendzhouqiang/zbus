package org.zbus.client.rpc;

import org.zbus.remoting.Message;

public interface Codec {
	Message encode(Request request);
	Message encode(Response response);
	<T> T decode(Message msg, String encoding);
}
