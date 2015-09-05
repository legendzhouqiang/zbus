package org.zbus.kit.json.impl;

import java.lang.reflect.Method;

import org.zbus.kit.json.JsonObject;

public class CastKit { 
	
	public static Integer intValue(Object value) {
		if(value == null) return null; 
		try { return Integer.valueOf(value.toString()); } catch(Exception e){ } 
		return null;
	}

	public static Long longValue(Object value) {
		if(value == null) return null;
		try { return Long.valueOf(value.toString()); } catch(Exception e){ } 
		return null;
	}

	public static Short shortValue(Object value) {
		if(value == null) return null;
		try { return Short.valueOf(value.toString()); } catch(Exception e){ } 
		return null;
	}

	public static Float floatValue(Object value) {
		if(value == null) return null;
		try { return Float.valueOf(value.toString()); } catch(Exception e){ } 
		return null;
	}

	public static Double doubleValue(Object value) {
		if(value == null) return null;
		try { return Double.valueOf(value.toString()); } catch(Exception e){ } 
		return null;
	}

	public static Byte byteValue(Object value) {
		if(value == null) return null;
		try { return Byte.valueOf(value.toString()); } catch(Exception e){ } 
		return null;
	}

	public static Character charValue(Object value) {
		if(value == null) return null;
		try { return Character.valueOf(value.toString().charAt(0)); } catch(Exception e){ } 
		return null;
	}

	public static String stringValue(Object value) {
		if(value == null) return null;
		return value.toString();
	}

	public static Boolean booleanValue(Object value) {
		if(value == null) return null;
		try { return Boolean.valueOf(value.toString()); } catch(Exception e){ } 
		return null;
	}

	public static <T extends Object> T objectValue(JsonObject json, Class<T> clazz) {
		if (json == null || clazz == null || clazz.isInterface()) return null;
		
		Method[] methods = clazz.getMethods();
		T result = newInstance(clazz);
		for (Method m : methods) {
			String name = m.getName();
			if(name.length() < 4) continue;
			if (!name.startsWith("set") || m.getParameterTypes().length > 1) continue;
			
			String k = String.format("%s%s", Character.toLowerCase(name.charAt(3)), name.substring(4));
			if (!json.containsKey(k)) continue;
			
			Class<?> paramClazz = m.getParameterTypes()[0];
			try {
				Method m2 = CastKit.class.getMethod(castMethod(paramClazz), Object.class);
				if (m2 == null) continue;
				
				Object args = m2.invoke(null, json.get(k));
				if (null != args) {
					m.invoke(result, args);
				}
			} catch (Exception e) {
				continue;
			}
		}
		return result;
	} 
	
	public static boolean isPrimitive(Class<?> clazz) {
		if(clazz == null) return false;
		
		if(clazz == Byte.class) return true;
		if(clazz == Character.class) return true;
		if(clazz == Short.class) return true;
		if(clazz == Integer.class) return true;
		if(clazz == Long.class) return true;
		if(clazz == Float.class) return true;
		if(clazz == Double.class) return true;
		if(clazz == String.class) return true; 
		
		return false;
	} 

	private static String castMethod(Class<?> clazz) {
		if (isPrimitive(clazz)) {
			String temp = clazz.getSimpleName();
			if (temp.equals("Integer")) {
				temp = "int";
			} else if (temp.equals("Character")) {
				temp = "char";
			}
			return String.format("%s%sValue", Character.toLowerCase(temp.charAt(0)), temp.substring(1));
		}
		return "objectValue";
	}
 
	public static <T extends Object> T newInstance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (Exception e) { 
			throw new IllegalArgumentException(e.getMessage(), e);
		}   
	}  
}