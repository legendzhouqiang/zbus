package org.zbus.kit.json;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class JSON {

	public static <T extends Object> T parseObject(String text, Class<T> clazz) {
		if(text == null || text.isEmpty()) return null;
		if(clazz == null || clazz.isInterface()) return null;
		
		return CastKit.objectValue(parseJSONObject(text), clazz);
	}

	public static JSONArray parseJSONArray(String text) {
		if (isEmpty(text) || !isJSONArray(text)) return null;
		return JSONArray.class.cast(parseJSON(text));
	}

	public static JSONObject parseJSONObject(String text) {
		if (isEmpty(text) || !isJSONObject(text)) return null;
		return JSONObject.class.cast(parseJSON(text));
	}

	public static String toJSONString(Object object) {
		if (object == null) return null;
		
		if (CastKit.isPrimitive(object.getClass())) {
			return String.class.isInstance(object) ? String.format("\"%s\"", object.toString()) : object.toString();
		} else if (object.getClass().isArray() || Collection.class.isInstance(object)) {
			return arrayToJSONString(object);
		} else if (Map.class.isInstance(object)) {
			return mapToJSONString(object);
		} else {
			return beanToJSONString(object);
		}
	}

	private static String mapToJSONString(Object object) {
		StringBuilder builder = new StringBuilder("{");
		Map<?, ?> map = Map.class.cast(object);
		Iterator<?> iterator = map.keySet().iterator();
		while (iterator.hasNext()) {
			Object k = iterator.next();
			if (CastKit.isPrimitive(k.getClass())) {
				Object v = map.get(k);
				String newK = toJSONString(k);
				builder.append(String.format("%s:%s,", newK.startsWith("\"") ? newK : String.format("\"%s\"", newK), toJSONString(v)));
			}
		}
		if (builder.charAt(builder.length() - 1) == ',')
			builder.setLength(builder.length() - 1);
		builder.append("}");
		return builder.toString();
	}

	private static String arrayToJSONString(Object object) {
		if (Collection.class.isInstance(object))
			object = List.class.cast(object).toArray();
		int count = Array.getLength(object);
		StringBuilder builder = new StringBuilder("[");
		for (int i = 0; i < count; i++) {
			builder.append(String.format("%s,", toJSONString(Array.get(object, i))));
		}
		if (builder.charAt(builder.length() - 1) == ',')
			builder.setLength(builder.length() - 1);
		builder.append("]");
		return builder.toString();
	}

	private static String beanToJSONString(Object object) {
		StringBuilder builder = new StringBuilder("{");
		Method[] methods = object.getClass().getMethods();
		for (Method method : methods) {
			if (!method.getName().startsWith("get") || method.getParameterTypes().length > 0 || method.getName().equals("getClass"))
				continue;
			String name = String.format("%s%s", Character.toLowerCase(method.getName().charAt(3)), method.getName().substring(4));
			try {
				Object result = method.invoke(object);
				if (null != result) {
					builder.append(String.format("\"%s\":%s,", name, toJSONString(result)));
				}
			} catch (Exception e) {
				continue;
			}
		}
		if (builder.charAt(builder.length() - 1) == ',')
			builder.setLength(builder.length() - 1);
		builder.append("}");
		return builder.toString();
	}

	private static boolean isJSONObject(String text) {
		text = text.trim();
		return text.startsWith("{") && text.endsWith("}");
	}

	private static boolean isJSONArray(String text) {
		text = text.trim();
		return text.startsWith("[") && text.endsWith("]");
	}

	private static boolean isEmpty(String text) {
		return null == text || text.isEmpty();
	}

	public static Object parseJSON(String text) {
		if(isEmpty(text)) return null;
		
		text.trim();
		Stack<String> stack = new Stack<String>();
		Stack<Object> objects = new Stack<Object>();
		Stack<String> keys = new Stack<String>();
		Stack<Boolean> types = new Stack<Boolean>();
		int idx = 0;
		int pos = 0;
		Object obj = null;
		do {
			switch (text.charAt(idx++)) {
			case '[':
			case '{':
				boolean isMap = text.charAt(idx - 1) == '{';
				if (stack.size() != 0 && stack.lastElement().equals("\"")) {
					break;
				}
				stack.push("}");
				types.push(isMap);
				if (null != obj)
					objects.push(obj);
				obj = CastKit.newInstance(isMap ? JSONObject.class : JSONArray.class);
				pos = idx;
				break;
			case ']':
			case '}':
				if (pos != idx) {
					String value = text.substring(pos, idx - (text.charAt(idx - 2) == '"' ? 2 : 1)).trim();
					if (obj instanceof HashMap) {
						JSONObject.class.cast(obj).put(keys.pop(), value);
					} else {
						JSONArray.class.cast(obj).add(value);
					}
				}
				types.pop();
				stack.pop();
				if (objects.size() > 0) {
					if (objects.lastElement() instanceof HashMap) {
						JSONObject.class.cast(objects.lastElement()).put(keys.pop(), obj);
					} else {
						JSONArray.class.cast(objects.lastElement()).add(obj);
					}
					obj = objects.pop();
				}
				pos = idx + 1;
				break;
			case '"':
				if (stack.lastElement().equals("\"")) {
					stack.pop();
				} else {
					stack.push("\"");
					pos = idx;
				}
				break;
			case ',':
				if (pos != idx) {
					String value = text.substring(pos, idx - (text.charAt(idx - 2) == '"' ? 2 : 1)).trim();
					if (types.lastElement()) {
						JSONObject.class.cast(obj).put(keys.pop(), value);
					} else {
						JSONArray.class.cast(obj).add(value);
					}
				}
				pos = idx;
				break;
			case ':':
				keys.push(text.substring(pos, (pos = idx) - 2));
				break;
			}
		} while (idx < text.length());
		return obj;
	}
}


class CastKit {
	
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

	public static <T extends Object> T objectValue(JSONObject json, Class<T> clazz) {
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