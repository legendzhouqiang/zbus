package org.zbus.kit.json;

import java.util.HashMap;

public class JSONObject extends HashMap<String, Object> {  
	private static final long serialVersionUID = -3007199945096476930L;
	
	public Integer getInt(String key) {
		return CastKit.intValue(get(key));
	}
	public Long getLong(String key) {
		return CastKit.longValue(key);
	}
	public Short getShort(String key) {
		return CastKit.shortValue(get(key));
	} 
	public Float getFloat(String key) {
		return CastKit.floatValue(get(key));
	} 
	public Double getDouble(String key) {
		return CastKit.doubleValue(get(key));
	} 
	public Byte getByte(String key) {
		return CastKit.byteValue(get(key));
	} 
	public Boolean getBool(String key) {
		return CastKit.booleanValue(get(key));
	}
	public Character getChar(String key) {
		return CastKit.charValue(get(key));
	}
	public String getString(String key) {
		return CastKit.stringValue(get(key));
	}
	public <T> T getBean(String key, Class<T> clazz) {
		return CastKit.objectValue(getJSONObject(key), clazz);
	}

	public JSONObject getJSONObject(String key) {
		Object obj = get(key);
		return obj instanceof JSONObject ? JSONObject.class.cast(obj) : null;
	}

	public JSONArray getJSONArray(String key) {
		Object obj = get(key);
		return obj instanceof JSONArray ? JSONArray.class.cast(obj) : null;
	}

	public <T> T toBean(Class<T> clazz) {
		return CastKit.objectValue(this, clazz);
	}
	
	public String toJSONString() {
		return JSON.toJSONString(this);
	}

	public String toString() {
		return toJSONString();
	}
}
