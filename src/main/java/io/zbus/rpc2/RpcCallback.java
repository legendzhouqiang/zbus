package io.zbus.rpc2;


public interface RpcCallback<T> { 
	void onSuccess(T result);  
	void onError(Exception error);  
}