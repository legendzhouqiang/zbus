package org.zbus.tc;
 
import java.util.Map; 
import org.zbus.client.Broker;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.service.Service;
import org.zbus.client.service.ServiceConfig;
import org.zbus.client.service.ServiceHandler;
import org.zbus.remoting.Helper;
import org.zbus.remoting.Message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eno.ENOInterface.ENOMsg;
import com.eno.InfoAgent.TCRSField;
import com.eno.etcpool.ENOProxy;
import com.eno.tcrs.ENORecordset;

class TCProxyHandler implements ServiceHandler {
 
	public static String toParamString(JSONObject paras) {
		if (paras == null)
			return "";
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (Map.Entry<String, Object> entry : paras.entrySet()) {
			String value = (String)entry.getValue();
			if (value != null) {
				if (i > 0) {
					sb.append("&");
				}
				sb.append(entry.getKey()).append("=").append(value);
				i++;
			}
		}
		return sb.toString();
	}
	 
	public Message handleRequest(Message req) { 
		Message res = new Message(); 
		JSONObject jsonRes = new JSONObject();
		try {
			JSONObject json = JSON.parseObject(req.getBodyString());
			
			int serviceId = json.getIntValue("service_id");
			int mainFuncId = json.getIntValue("main_funcid");
			int subFuncId = json.getIntValue("sub_funcid");
			JSONObject params = json.getJSONObject("params");
			String paramString = toParamString(params);
			
			
			ENOMsg msg = new ENOMsg(); 
			byte[] data = ENOProxy.getData(serviceId, mainFuncId, subFuncId,
					paramString.getBytes(), "", true, msg);
			
			
			if(data == null){
				jsonRes.put("error_code", "500"); 
				jsonRes.put("error_msg",msg.getMessage());
				res.setBody(JSON.toJSONString(jsonRes));
				return res;
			}
			
			ENORecordset[] mrs = ENORecordset.constructMR(data, 0);

			if (mrs == null || mrs.length == 0) {
				jsonRes.put("error_code", "500"); 
				jsonRes.put("error_msg",msg.getMessage());
				res.setBody(JSON.toJSONString(jsonRes));
				return res;
			}
 
			if (mrs[0].isErrorInfo()) { //error
				mrs[0].fetchFirst();
				jsonRes.put("error_code", "500");  
				jsonRes.put("error_msg", mrs[0].getString(0));
				res.setBody(JSON.toJSONString(jsonRes));
				return res; 
			}
			JSONArray rsArrayJson = new JSONArray();
			for(int rsIdx=0; rsIdx<mrs.length; rsIdx++){ 
				ENORecordset rs = mrs[rsIdx];
				int cols = rs.fieldCount();
				rs.fetchFirst();   
				JSONArray rsJson = new JSONArray();
				while (!rs.isEOF()) { 
					JSONObject row = new JSONObject();
					for (int i = 0; i < cols; i++) {
						TCRSField field = rs.fieldDesc(i);
						String key = field.fieldName;
						String val = rs.toString(i); //damned ENO's bugs and fucking design
						if(val == null){
							val = "";
						}
						row.put(key, val); 
					} 
					rsJson.add(row); 
					rs.fetchNext();
				} 
				rsArrayJson.add(rsJson);
			} 
			
			jsonRes.put("result", rsArrayJson);   
			res.setBody(JSON.toJSONString(jsonRes));
			return res;  

		} catch (Exception e) { 
			jsonRes.put("error_code", "500");  
			jsonRes.put("error_msg", e.getMessage());
			res.setBody(JSON.toJSONString(jsonRes));
			return res; 
		}
	}

	
}

public class TCProxy {  
	public static void main(String[] args) throws Exception{   
		String address = Helper.option(args, "-b", "127.0.0.1:15555"); 
		int threadCount = Helper.option(args, "-c", 1);
		String service = Helper.option(args, "-s", "ETC-172.24.180.71");
		
		ServiceConfig config = new ServiceConfig();
		config.setThreadCount(threadCount); 
		config.setMq(service);
		
		//配置Broker
		SingleBrokerConfig brokerCfg = new SingleBrokerConfig();
		brokerCfg.setBrokerAddress(address);
		Broker broker = new SingleBroker(brokerCfg);
		config.setBroker(broker);
		config.setServiceHandler(new TCProxyHandler());
		Service svc = new Service(config);
		svc.start();   
	} 
}
