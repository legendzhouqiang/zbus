package org.zbus.kit.json.impl;

import java.util.ArrayList;

import org.zbus.kit.json.Json;

public class JsonArray extends ArrayList<Object> {  
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
		return CastKit.objectValue(getJsonObject(i), clazz);
	}

	public JsonObject getJsonObject(int i) {
		Object obj = get(i);
		return obj instanceof JsonObject ? JsonObject.class.cast(obj) : null;
	}

	public JsonArray getJsonArray(int i) {
		Object obj = get(i);
		return obj instanceof JsonArray ? JsonArray.class.cast(obj) : null;
	}

	public String toJsonString() {
		return Json.toJson(this);
	}

	public String toString() {
		return toJsonString();
	}

}
