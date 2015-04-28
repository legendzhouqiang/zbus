package org.zstacks.zbus.proxy.kcxp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.ZbusException;
import org.zstacks.zbus.client.service.Caller;
import org.zstacks.znet.Message;

import sun.misc.BASE64Decoder;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@SuppressWarnings("restriction")
public class KCXPClient extends Caller {
	private int timeout = 2500;
	private String encoding = "gbk";
	
	private final BASE64Decoder base64Decoder = new BASE64Decoder() ; 
	
	public KCXPClient(Broker broker, String mq) {
		super(broker, mq);
	}

	/**
	 * 参数只支持String类型，byte[]类型需要：
	 *  1）Value值特殊专户为Base64编码
	 *  2）Key值特殊标示： ^原Key^
	 *  举例说明:
	 *  ^image^: base64_string 
	 * @param funcId
	 * @param params
	 * @return
	 */
	public KCXPResult request(String funcId, Map<String, String> params) { 
		JSONObject reqJson = new JSONObject();
		reqJson.put("method", funcId);
		JSONArray paramArray = new JSONArray();
		paramArray.add(params);
		reqJson.put("params", paramArray);

		Message req = new Message();
		req.setBody(JSON.toJSONString(reqJson)); 
		
		try {
			Message res = this.invokeSync(req, this.timeout);
			if(res == null){
				throw new ZbusException(String.format("[Timeout] Request KCXP funcId=%s", funcId));
			}
			JSONObject resJson = JSON.parseObject(res.getBodyString(this.encoding));
			KCXPResult result = new KCXPResult();
			result.setStatus("0");
			if(resJson.containsKey("error_code")){
				result.setStatus(resJson.getString("error_code"));
				result.setErrorMessage(resJson.getString("error_msg"));
				return result;
			}
			JSONArray resultsetArray = resJson.getJSONArray("result");
			Iterator<Object> rsIter = resultsetArray.iterator();
			while(rsIter.hasNext()){
				KCXPTable table = new KCXPTable();
				JSONArray resultset = (JSONArray) rsIter.next(); 
				Iterator<Object> rowIter = resultset.iterator();
				while(rowIter.hasNext()){
					JSONObject row = (JSONObject) rowIter.next(); 
					Map<String, byte[]> data = new HashMap<String, byte[]>(); 
					for(Map.Entry<String, Object> e : row.entrySet()){
						byte[] value = this.base64Decoder.decodeBuffer((String)e.getValue());
						data.put(e.getKey().trim(), value);
					}
					table.addRow(data);
				}
				result.addTable(table);
			}
			return result;
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

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	} 
	
	
}
