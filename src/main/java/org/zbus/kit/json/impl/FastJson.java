package org.zbus.kit.json.impl;

import org.zbus.kit.json.JsonConvertor;
import org.zbus.kit.log.Logger;

import com.alibaba.fastjson.JSON;

public class FastJson implements JsonConvertor {
	private static final Logger log = Logger.getLogger(FastJson.class);
	
	public FastJson(){
		log.info("Using fastjson");
	}
	@Override
	public String toJson(Object value) { 
		return JSON.toJSONString(value);
	}

	@Override
	public Object parseJson(String text) { 
		return JSON.parse(text); 
	}

	@Override
	public <T> T parseObject(String text, Class<T> clazz) { 
		return JSON.parseObject(text, clazz);
	}
}
