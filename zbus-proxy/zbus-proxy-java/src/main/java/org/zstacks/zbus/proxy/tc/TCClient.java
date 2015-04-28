package org.zstacks.zbus.proxy.tc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.ZbusException;
import org.zstacks.zbus.client.service.Caller;
import org.zstacks.znet.Message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;


public class TCClient extends Caller {  
	private int timeout = 2500;
	private String encoding = "GBK";

	public TCClient(Broker broker, String mq) {
		super(broker, mq);
	}

	/**
	 * 
	 * 返回结果集JSON格式 [ Table[Row{k1:v1},...], [{k1:v1}...] ]
	 * 
	 * @param serviceId 服务号
	 * @param mainFuncId 主功能号
	 * @param subFuncId 子功能号
	 * @param params KV参数
	 * @return
	 */
	public JSONArray requestJson(String serviceId, String mainFuncId,
			String subFuncId, Map<String, String> params) {
		JSONObject json = new JSONObject();
		json.put("service_id", serviceId);
		json.put("main_funcid", mainFuncId);
		json.put("sub_funcId", subFuncId);
		json.put("params", params);
		
		Message req = new Message();
		req.setJsonBody(JSON.toJSONString(json));
		
		try {
			Message res = this.invokeSync(req, this.timeout);
			JSONObject jsonRes = JSON.parseObject(res.getBodyString(this.encoding));
			if(jsonRes.containsKey("error_code")){
				String errorCode = jsonRes.getString("error_code");
				String errorMsg = jsonRes.getString("error_msg");
				throw new ZbusException(String.format("error=%s, msg=%s", errorCode, errorMsg));
			}
			return jsonRes.getJSONArray("result");
		} catch (IOException e) { 
			throw new ZbusException(e.getMessage(), e);
		}
	}

	public TCResult request(String serviceId, String mainFuncId,
			String subFuncId, Map<String, String> params) {
		
		JSONArray rsArrayJson = (JSONArray)this.requestJson(serviceId, mainFuncId, subFuncId, params);
		
		TCResult tcRes = new TCResult();
		for(Object rsObj : rsArrayJson){
			TCTable table = new TCTable();
			JSONArray rsJson = (JSONArray)rsObj; 
			for(Object obj : rsJson){
				JSONObject objJson = (JSONObject)obj;
				Map<String, byte[]> map = new HashMap<String, byte[]>();
				for(Entry<String, Object> e :objJson.entrySet()){
					String v = (String)e.getValue();
					try {
						map.put(e.getKey(), v.getBytes(this.encoding));
					} catch (UnsupportedEncodingException e1) { 
						e1.printStackTrace();
					}
				}
				table.addRow(map);
			}
			tcRes.addTable(table);
		}
		return tcRes; 
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
