package org.zbus.client.rpc.json;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

import org.zbus.client.ZbusException;
import org.zbus.logging.Logger;
import org.zbus.logging.LoggerFactory;
import org.zbus.remoting.Message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JsonHelper{ 
	private static final Logger log = LoggerFactory.getLogger(JsonHelper.class); 
	
    public static final byte[] toJSONBytes(Object object, String charsetName, SerializerFeature... features) {
        SerializeWriter out = new SerializeWriter();

        try {
            JSONSerializer serializer = new JSONSerializer(out);
            for (SerializerFeature feature : features) {
                serializer.config(feature, true);
            }

            serializer.write(object);

            return out.toBytes(charsetName);
        } finally {
            out.close();
        }
    }
    
    
    public static Message packJsonRequest(JsonRequest req){
		Message msg = new Message();
		JSONObject json = new JSONObject(); 
		json.put("module", req.getModule());
		json.put("method", req.getMethod());
		json.put("params", req.getParams()); 
		Class<?>[] types = req.getParamTypes();
		if(types != null){
			String[] paramTypeStrings = new String[types.length];
			for(int i=0;i<types.length;i++){
				paramTypeStrings[i]=types[i].getCanonicalName();
			}
			json.put("paramTypes", paramTypeStrings);
		} 
		msg.setEncoding(req.getEncoding());
		msg.setBody(JsonHelper.toJSONBytes(json,
					req.getEncoding(),
					SerializerFeature.WriteMapNullValue,
					SerializerFeature.WriteClassName));
		return msg;
	}
	
    public static JSONObject unpackReplyJson(Message msg){
    	return unpackReplyJson(msg, "UTF-8");
    }
	public static JSONObject unpackReplyJson(Message msg, String encoding) { 
		JSONObject res = null;
		String text = "";
		try {
			text = msg.getBodyString(encoding);
			res = (JSONObject) JSON.parse(text);
		} catch (Exception e) {
			text = text.replace("@type", "unknown-class"); //try disable class feature
			try{
				res = (JSONObject) JSON.parse(text);
			} catch (JSONException ex) { 
				new ZbusException("json error: "+ text); 
			}  
		}
		return res;
		 
	}
    
	public static <T> T unpackReplyObject(Message msg){
		return unpackReplyObject(msg, "UTF-8");
	}
	
    @SuppressWarnings("unchecked")
	public static <T> T unpackReplyObject(Message msg, String encoding) {
		//消息体约定为JSON数据,不管是错误消息还是正常消息
		JSONObject res = null;
		String text = "";
		try {
			text = msg.getBodyString(encoding);
			res = (JSONObject) JSON.parse(text);
		} catch (Exception e) {
			text = text.replace("@type", "unknown-class"); //try disable class feature
			try{
				res = (JSONObject) JSON.parse(text);
			} catch (JSONException ex) { 
				new ZbusException("json error: "+ text); 
			}  
			if(res.containsKey("stack_trace")){
				log.info(res.getString("stack_trace"));
			}
			
			if(res.containsKey("error")){ 
				JSONObject error = res.getJSONObject("error");
				if(error.containsKey("message")){
					throw new ZbusException(error.getString("message"));
				}
			} 
			throw new ZbusException("json error: "+ text); 
		}
		
		if( !msg.isStatus200() ){ //正常消息
			
			if(res.containsKey("stack_trace")){
				log.info(res.getString("stack_trace"));
			}
			
			if(res.containsKey("error")){ 
				Object error = res.get("error"); 
				if(error instanceof RuntimeException){ //exception can be loaded
					throw (RuntimeException)error; 
				}  	
				throw new ZbusException(error.toString());
			} 
			
			throw new ZbusException("return json invalid");
		}
		
		if (!res.containsKey("result")){
			throw new ZbusException("return json invalid, result required");
		}
		
		//合法返回消息
		Object result = res.get("result");
		if(!(result instanceof JSONArray )){
			return (T)result;
		}
		String returnType = res.getString("returnType");
		if(returnType==null || returnType.trim().length() == 0){
			return (T)result;
		}
		
		//指定returnType
		try{
			String resultStr = res.getString("result");
			Class<?> clz = Class.forName(returnType);
			if(Collection.class.isAssignableFrom(clz)){
				return (T)result;
			} 
			
			List<?> resList = JSONArray.parseArray(resultStr, clz);
			int size = resList.size();
			T t= (T)Array.newInstance(clz, size);
			System.arraycopy(resList.toArray(), 0, t, 0,size);
			return t;
			
		} catch(Exception e) {
			return (T)result; 
		} 
	}

}
