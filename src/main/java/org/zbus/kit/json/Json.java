package org.zbus.kit.json;

import org.zbus.kit.json.impl.DefaultJson;
import org.zbus.kit.log.Logger;

public class Json {
	private static final Logger log = Logger.getLogger(Json.class);
	private static JsonConvertor convertor;
	
	static {
		initDefaultJsonImpl();
	}
	
	public static void setJsonConvertor(JsonConvertor convertor) {
		if (convertor != null) {
			Json.convertor = convertor;
		}
		
	}  
	
	public static void initDefaultJsonImpl() {
		if (convertor != null){
			return ;
		}
		
		String defaultConvertor = String.format("%s.impl.FastJson", Json.class.getPackage().getName());
		try {
			//default to fastjson
			Class.forName("com.alibaba.fastjson.JSON");
			Class<?> convertorClass = Class.forName(defaultConvertor);
			Json.convertor = (JsonConvertor)convertorClass.newInstance();
			log.info("Using alibaba fastjon");
		} catch (Exception e) {  
			log.info("Using zbus DefaultJson");
			Json.convertor = new DefaultJson();
		}
	}
	
	public static String toJson(Object value){
		return convertor.toJson(value);
	}
	
	public static Object parseJson(String text){
		return convertor.parseJson(text);
	}

	public static <T> T parseObject(String text, Class<T> clazz){
		return convertor.parseObject(text, clazz);
	}
}
