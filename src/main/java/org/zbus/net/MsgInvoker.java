package org.zbus.net;

import java.io.IOException;

public interface MsgInvoker<REQ extends Id, RES> { 
	/**
	 * 同步消息模式
	 * 
	  @param req
	 * @param timeout
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	RES invokeSync(REQ req, int timeout) throws IOException, InterruptedException;

	/**
	 * 异步消息模式 
	 * 
	 * @param msg
	 * @param callback
	 * @throws IOException
	 */
	void invokeAsync(REQ req, ResultCallback<RES> callback) throws IOException;
}
