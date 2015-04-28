package org.zstacks.zbus.proxy;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.rpc.Rpc;
import org.zstacks.zbus.proxy.kcxp.KCXPClient;
import org.zstacks.zbus.proxy.kcxp.KCXPResult;
import org.zstacks.zbus.proxy.tc.TCClient;
import org.zstacks.zbus.proxy.tc.TCResult;
import org.zstacks.zbus.proxy.trade.Request;
import org.zstacks.zbus.proxy.trade.Response;
import org.zstacks.zbus.proxy.trade.TradeClient;
import org.zstacks.znet.Message;
import org.zstacks.znet.ticket.ResultCallback;

import com.alibaba.fastjson.JSON;

public class ZbusUtil {
	private static Logger log = LoggerFactory.getLogger(ZbusUtil.class);
	private Broker broker;

	private Rpc rpc;
	private KCXPClient kcxp;
	private TCClient tc;
	private TradeClient trade;

	
	public Object invoke(String methodName, Object... args) {
		try {
			return rpc.invokeSync(methodName, args);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("zbus rpc call error(methodname=" + methodName+ ";args:" + args);
			return null;
		}
	}

	
	/**
	 * 同步消息
	 */
	public Message invokeSync(String serviceName, String token, Message req, int zbusTimeout) {
		req.setMq(serviceName);
		req.setToken(token); 
		try {
			return broker.invokeSync(req, zbusTimeout);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("zbus mq send error(serviceName=" + serviceName
					+ ";token:" + token + ";req:" + JSON.toJSON(req)
					+ ";zbusTimeout:" + String.valueOf(zbusTimeout));
			return null;
		} 
	}


	public void invokeAsync(Message msg, ResultCallback callback) { 
		try {
			broker.invokeAsync(msg, callback);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("zbus mq send error msg:" + JSON.toJSON(msg)); 
		}
	}

	/****
	 * 调用TC的zbus客户端访问方法
	 * 
	 * @param serviceId
	 * @param mainFuncid
	 * @param subFuncid
	 * @param params
	 * @return
	 */
	public TCResult request(String serviceId, String mainFuncid,
			String subFuncid, Map<String, String> params) {
		try {
			return tc.request(serviceId, mainFuncid, subFuncid, params);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("tc proxy request error(serviceId=" + serviceId
					+ ";mainFuncid:" + mainFuncid + ";subFuncid:" + subFuncid
					+ ";params:" + JSON.toJSON(params));
			return null;
		}
	}

	/****
	 * 调用柜台接口的zbus客户端方法
	 * 
	 * @param algorithm
	 * @param publicKey
	 * @param privateKey
	 * @param password
	 * @return
	 */
	public String decrypt(String algorithm, String publicKey,
			String privateKey, String password) {
		try {
			return trade.decrypt(algorithm, publicKey, privateKey, password);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("trade client error.");
			return null;
		}
	}

	/*******
	 * 调用柜台接口的zbus客户端方法 encrypt
	 * 
	 * @param algorithm
	 * @param password
	 * @param key
	 * @return
	 */
	public String encrypt(String algorithm, String password, String key) {
		try {
			return trade.encrypt(algorithm, password, key);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("trade client error.");
			return null;
		}
	}

	/********
	 * 调用柜台接口的zbus客户端方法 trade
	 * 
	 * @param request
	 * @return
	 */
	public Response trade(Request request) {
		try {
			return trade.trade(request);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("trade client error.");
			return null;
		}
	}

	/**
	 * kcxp的zbus客户端调用方法
	 * 
	 * @param funcId
	 * @param params
	 * @return
	 */
	public KCXPResult request(String funcId, Map<String, String> params) {
		try {
			return kcxp.request(funcId, params);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("KCXPClient client error.");
			return null;
		}
	}

	/***
	 * kcxp的zbus客户端调用方法
	 * 
	 * @param funcId
	 * @param params
	 * @return
	 */
	public KCXPResult requestKcxp(String funcId, Map<String, String> params) {
		try {
			return kcxp.request(funcId, params);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error("KCXPClient client error.");
			return null;
		}
	}
 
	public Rpc getRpc() {
		return rpc;
	}

	public void setRpc(Rpc rpc) {
		this.rpc = rpc;
	}

	public KCXPClient getKcxp() {
		return kcxp;
	}

	public void setKcxp(KCXPClient kcxp) {
		this.kcxp = kcxp;
	}

	public TCClient getTc() {
		return tc;
	}

	public void setTc(TCClient tc) {
		this.tc = tc;
	}

	public TradeClient getTrade() {
		return trade;
	}

	public void setTrade(TradeClient trade) {
		this.trade = trade;
	}
}
