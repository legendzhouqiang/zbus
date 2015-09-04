package org.zbus.kit.json;

import java.util.ArrayList;

public class JSONArray extends ArrayList<Object> {  
	private static final long serialVersionUID = 6699808953410831664L;

	public Integer getInt(int i) {
		return CastKit.intValue(get(i));
	} 
	public Long getLong(int i) {
		return CastKit.longValue(i);
	} 
	public Short getShort(int i) {
		return CastKit.shortValue(get(i));
	}
	public Float getFloat(int i) {
		return CastKit.floatValue(get(i));
	}  
	public Double getDouble(int i) {
		return CastKit.doubleValue(get(i));
	} 
	public Byte getByte(int i) {
		return CastKit.byteValue(get(i));
	} 
	public Boolean getBool(int i) {
		return CastKit.booleanValue(get(i));
	} 
	public Character getChar(int i) {
		return CastKit.charValue(get(i));
	}  
	public String getString(int i) {
		return CastKit.stringValue(get(i));
	}
	public <T> T getBean(int i, Class<T> clazz) {
		return CastKit.objectValue(getJSONObject(i), clazz);
	}

	public JSONObject getJSONObject(int i) {
		Object obj = get(i);
		return obj instanceof JSONObject ? JSONObject.class.cast(obj) : null;
	}

	public JSONArray getJSONArray(int i) {
		Object obj = get(i);
		return obj instanceof JSONArray ? JSONArray.class.cast(obj) : null;
	}

	public String toJSONString() {
		return JSON.toJSONString(this);
	}

	public String toString() {
		return toJSONString();
	}

}
