package org.zbus.rpc;

import org.zbus.mq.MqException;

public class RpcException extends MqException { 
	private static final long serialVersionUID = 8445590420018236422L;

	public RpcException(String message) {
		super(message); 
	}

	public RpcException() { 
	}

	public RpcException(String message, Throwable cause) {
		super(message, cause); 
	}

	public RpcException(Throwable cause) {
		super(cause); 
	}

}
