package org.zbus.kit.json.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.zbus.kit.json.JsonArray;
import org.zbus.kit.json.JsonConvertor;
import org.zbus.kit.json.JsonObject;
import org.zbus.kit.log.Logger;

public class DefaultJson implements JsonConvertor {
	private static final Logger log = Logger.getLogger(DefaultJson.class);
	public static String TimestampPattern = "yyyy-MM-dd HH:mm:ss";
	public static String DatePattern = "yyyy-MM-dd";
	
	public DefaultJson(){
		log.info("Using Default Json");
	}
	
	@SuppressWarnings("unchecked")
	public String toJson(Object value) {
		if(value == null) return "null";
		
		if(value instanceof String)
			return "\"" + escape((String)value) + "\"";
		
		if(value instanceof Double){
			if(((Double)value).isInfinite() || ((Double)value).isNaN())
				return "null";
			else
				return value.toString();
		}
		
		if(value instanceof Float){
			if(((Float)value).isInfinite() || ((Float)value).isNaN())
				return "null";
			else
				return value.toString();
		}
		
		if(value instanceof Number)
			return value.toString();
		
		if(value instanceof Boolean)
			return value.toString();
		
		if (value instanceof java.util.Date) {
			if (value instanceof java.sql.Timestamp)
				return "\"" + new SimpleDateFormat(TimestampPattern).format(value) + "\"";
			if (value instanceof java.sql.Time)
				return "\"" + value.toString() + "\"";
			return "\"" + new SimpleDateFormat(DatePattern).format(value) + "\"";
		}
		
		if(value instanceof Map) {
			return mapToJson((Map<Object,Object>)value);
		}
		
		if(value instanceof List) {
			return listToJson((List<Object>)value);
		}
		
		if (value instanceof Character) {
			return "\"" + escape(value.toString()) + "\"";
		} 
		
		if (value instanceof Object[]) {
			Object[] arr = (Object[])value;
			List<Object> list = new ArrayList<Object>(arr.length);
			for (int i=0; i<arr.length; i++)
				list.add(arr[i]);
			return listToJson(list);
		}
		
		if (value instanceof Enum) {
			return "\"" + ((Enum<?>)value).toString() + "\"";
		}
		
		String result = beanToJson(value);  
		if (result != null) return result;
		
		//default to String
		return "\"" + escape(value.toString()) + "\"";
	}
	
	private String mapToJson(Map<Object,Object> map) {
		if(map == null) return "null";  
		
        StringBuilder sb = new StringBuilder();
        boolean first = true;
		Iterator<?> iter = map.entrySet().iterator(); 
		
        sb.append('{');
		while(iter.hasNext()){
            if(first) first = false;
            else sb.append(',');
            
			Map.Entry<?,?> entry = (Map.Entry<?,?>)iter.next();
			appendKV(String.valueOf(entry.getKey()),entry.getValue(), sb);
		}
        sb.append('}');
		return sb.toString();
	}
	
	private void appendKV(String key, Object value, StringBuilder sb){
		sb.append('\"');
        if(key == null) sb.append("null");
        else escape(key, sb);
		sb.append('\"').append(':'); 
		sb.append(toJson(value));  
	}

	private String listToJson(List<Object> list) {
		if(list == null) return "null";
		
        boolean first = true;
        StringBuilder sb = new StringBuilder();
		Iterator<Object> iter = list.iterator();
        
        sb.append('[');
		while(iter.hasNext()){
            if(first) first = false;
            else sb.append(',');
            
			Object value = iter.next();
			if(value == null){
				sb.append("null");
				continue;
			}
			sb.append(toJson(value));
		}
        sb.append(']');
		return sb.toString();
	} 
	
	private String beanToJson(Object model) {
		Map<Object,Object> map = new HashMap<Object,Object>();
		
		//support for public fields
		Field[] fields = model.getClass().getFields(); 
		for(Field f : fields){ 
			String attrName = f.getName();
			Object value = null;
			try { value = f.get(model); } catch (Exception e) {continue;}
			map.put(attrName, value); 
		}
		
		Method[] methods = model.getClass().getMethods();
		for (Method m : methods) {
			String methodName = m.getName();
			int indexOfGet = methodName.indexOf("get");
			if (indexOfGet == 0 && methodName.length() > 3) {	// Only getter
				String attrName = methodName.substring(3);
				if (!attrName.equals("Class")) {				// Ignore Object.getClass()
					Class<?>[] types = m.getParameterTypes();
					if (types.length == 0) {
						try {
							Object value = m.invoke(model);
							map.put(firstCharToLowerCase(attrName), value);
						} catch (Exception e) {
							throw new RuntimeException(e.getMessage(), e);
						}
					}
				}
			}
			else {
               int indexOfIs = methodName.indexOf("is");
               if (indexOfIs == 0 && methodName.length() > 2) {
                  String attrName = methodName.substring(2);
                  Class<?>[] types = m.getParameterTypes();
                  if (types.length == 0) {
                      try {
                          Object value = m.invoke(model);
                          map.put(firstCharToLowerCase(attrName), value);
                      } catch (Exception e) {
                          throw new RuntimeException(e.getMessage(), e);
                      }
                  }
               }
            }
		}
		return mapToJson(map);
	}
	
	private static String firstCharToLowerCase(String str) {
		char firstChar = str.charAt(0);
		if (firstChar >= 'A' && firstChar <= 'Z') {
			char[] arr = str.toCharArray();
			arr[0] += ('a' - 'A');
			return new String(arr);
		}
		return str;
	}
	
	
	private static String escape(String s) {
		if(s == null) return null;
        StringBuilder sb = new StringBuilder();
        escape(s, sb);
        return sb.toString();
    }
	
	private static void escape(String s, StringBuilder sb) {
		for(int i=0; i<s.length(); i++){
			char ch = s.charAt(i);
			switch(ch){
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '/':
				sb.append("\\/");
				break;
			default:
				if((ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u007F' && ch <= '\u009F') || (ch >= '\u2000' && ch <= '\u20FF')) {
					String str = Integer.toHexString(ch);
					sb.append("\\u");
					for(int k=0; k<4-str.length(); k++) {
						sb.append('0');
					}
					sb.append(str.toUpperCase());
				}
				else{
					sb.append(ch);
				}
			}
		}
	}
	
	
	public <T extends Object> T parseObject(String text, Class<T> clazz) {
		if(text == null || text.isEmpty()) return null;
		if(clazz == null || clazz.isInterface()) return null;
		
		return CastKit.objectValue((JsonObject)parseJson(text), clazz);
	}

	public Object parseJson(String text) {
		if(text == null) return null; 
		text = text.trim();
		if(text.isEmpty()) return null;
		
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
				obj = CastKit.newInstance(isMap ? JsonObject.class : JsonArray.class);
				pos = idx;
				break;
			case ']':
			case '}':
				if (pos != idx) {
					String value = text.substring(pos, idx - (text.charAt(idx - 2) == '"' ? 2 : 1)).trim();
					if (obj instanceof HashMap) {
						JsonObject.class.cast(obj).put(keys.pop(), value);
					} else {
						JsonArray.class.cast(obj).add(value);
					}
				}
				types.pop();
				stack.pop();
				if (objects.size() > 0) {
					if (objects.lastElement() instanceof HashMap) {
						JsonObject.class.cast(objects.lastElement()).put(keys.pop(), obj);
					} else {
						JsonArray.class.cast(objects.lastElement()).add(obj);
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
						JsonObject.class.cast(obj).put(keys.pop(), value);
					} else {
						JsonArray.class.cast(obj).add(value);
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