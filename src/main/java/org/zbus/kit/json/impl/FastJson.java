package org.zbus.kit.json.impl;

import java.util.List;

import org.zbus.kit.json.JsonConvertor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class FastJson implements JsonConvertor {

	@Override
	public String toJSONString(Object value) { 
		return JSON.toJSONString(value);
	}
	
	public byte[] toJsonBytes(Object value, String encoding){ 
		return toJSONBytes(value, encoding,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.WriteClassName);
	}
 
	public Object parseJson(String text) { 
		return JSON.parse(text); 
	}
	 

	@Override
	public <T> T parseObject(String text, Class<T> clazz) { 
		return JSON.parseObject(text, clazz);
	}
	
	@Override
	public <T> List<T> parseArray(String text, Class<T> clazz) {
		return JSON.parseArray(text, clazz);
	}
	
	private static final byte[] toJSONBytes(Object object, String charsetName,
			SerializerFeature... features) {
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
}
