package org.zstacks.zbus.proxy.trade;

import java.io.IOException;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.ZbusException;
import org.zstacks.zbus.client.service.Caller;
import org.zstacks.znet.Message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;



public class TradeClient extends Caller{   
	private final String DO_CRYPT = "_do_crypt_";
    private final String DECRYPT_ALGORITHM = "WANGAN";
    private final String DECRYPT_PUBLIC_KEY = "BF6C2C496593917FEEDFE0F6C62BA237C32A99886D66CC3D20DBAEB38484D001C86EE38576C6A92CA3C94C03B1AD284A0F85498D3DEB9134DFC57BABE8271401";
    private final String DECRYPT_PRIVATE_KEY = "2D160168583065B8C83E9AF204C30A363015BC8BD198C0CA350F091AE73F90EE321E8767FED9CAA9FDD58960436B320FF4B7CFD06BFDA418D31290CA40DAE0F1";

	
	private int timeout = 2500;
	private String encoding = "GBK";
	
	//private final BASE64Encoder base64Encoder = new BASE64Encoder() ; 

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}


	public TradeClient(Broker broker, String mq) {
		super(broker, mq);
	}
	 
	
	/**
	 * 
	 * @param algorithm AES or KDE
	 * @param password
	 * @param key
	 * @return
	 */
	public String encrypt(String algorithm, String password, String key){
		Message req = new Message();
		req.setHead(DO_CRYPT, "1");
		//{method: encrypt, params: {algorithm:KDE, password:xxx, key: yyy} } 
		JSONObject json = new JSONObject();
		json.put("method", "encrypt");
		JSONObject params = new JSONObject();
		params.put("algorithm", algorithm);
		params.put("password", password);//base64Encoder.encode(password.getBytes()));
		params.put("key", key); 
		json.put("params", params);
		
		req.setJsonBody(JSON.toJSONString(json));
		
		try {
			Message res = this.invokeSync(req, timeout);
			JSONObject jsonRes = JSON.parseObject(res.getBodyString(this.encoding));
			if(jsonRes.containsKey("result")){
				return jsonRes.getString("result");
			} else {
				String reason = "unknown";
				if(jsonRes.containsKey("error_msg")){
					reason = jsonRes.getString("error_msg");
				}
				throw new ZbusException(reason);
			}
		} catch (IOException e) {
			throw new ZbusException(e.getMessage(), e);
		}
	}
	
	public String decrypt(String password){
		return this.decrypt(DECRYPT_ALGORITHM, DECRYPT_PUBLIC_KEY, DECRYPT_PRIVATE_KEY, password);
	}
	/**
	 * 
	 * @param algorithm WANGAN
	 * @param password
	 * @param key
	 * @return
	 */
	public String decrypt(String algorithm, String publicKey, String privateKey, String password){
		Message req = new Message();
		req.setHead(DO_CRYPT, "1");
		//{method: decrypt, params: {algorithm:KDE, public:xxx, private: yyy, password: ****} } 
		JSONObject json = new JSONObject();
		json.put("method", "decrypt");
		JSONObject params = new JSONObject();
		params.put("algorithm", algorithm);
		params.put("public", publicKey);
		params.put("private", privateKey);
		params.put("password",password); 
		json.put("params", params);
		
		req.setJsonBody(JSON.toJSONString(json));
		
		try {
			Message res = this.invokeSync(req, timeout);
			JSONObject jsonRes = JSON.parseObject(res.getBodyString());
			if(jsonRes.containsKey("result")){
				return jsonRes.getString("result");
			} else {
				String reason = "unknown";
				if(jsonRes.containsKey("error_msg")){
					reason = jsonRes.getString("error_msg");
				}
				throw new ZbusException(reason);
			}
		} catch (IOException e) {
			throw new ZbusException(e.getMessage(), e);
		}
	}
	
	public Response trade(Request request){  
		Message req = new Message();
		req.setBody(request.toString());
		try {
			Message res = this.invokeSync(req, this.timeout);
			Response response = Response.load(res.getBodyString(this.encoding));
			return response;
		} catch (IOException e) {
			throw new ZbusException(e.getMessage(), e);
		}
	} 

	public int getTimeout() {
		return timeout;
	}


	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}

