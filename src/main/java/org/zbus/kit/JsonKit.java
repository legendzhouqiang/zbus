/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.kit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonKit { 
	static Method fastJsonMethod;
	static {
		try {
			Class<?> fastJsonClass = Class.forName("com.alibaba.fastjson.JSON");
			fastJsonMethod = fastJsonClass.getMethod("toJSONString", Object.class);
		} catch (Exception e) { 
			fastJsonMethod = null;
		} 
	}
	
	public static String toJson(Object value) {
		//try fastjson first
		if(fastJsonMethod != null){
			try {
				return (String)fastJsonMethod.invoke(null, value);
			} catch (Exception e){
				e.printStackTrace();
			}
		}  
		return JsonWriter.toJson(value, JsonWriter.maxDepth);
	}
}



class JsonWriter { 
	public static int maxDepth = 15;
	public static String tsPattern = "yyyy-MM-dd HH:mm:ss";
	public static String datePattern = "yyyy-MM-dd";
	
	public static void setMaxDepth(int maxDepth) {
		JsonWriter.maxDepth = maxDepth;
	}
	
	public static void setTimestampPattern(String tsPattern) {
		JsonWriter.tsPattern = tsPattern;
	} 
	
	public static void setDatePattern(String datePattern) { 
		JsonWriter.datePattern = datePattern;
	}
	
	public static String mapToJson(Map<Object,Object> map, int depth) {
		if(map == null) return "null";  
		
        StringBuilder sb = new StringBuilder();
        boolean first = true;
		Iterator<?> iter = map.entrySet().iterator(); 
		
        sb.append('{');
		while(iter.hasNext()){
            if(first) first = false;
            else sb.append(',');
            
			Map.Entry<?,?> entry = (Map.Entry<?,?>)iter.next();
			appendKV(String.valueOf(entry.getKey()),entry.getValue(), sb, depth);
		}
        sb.append('}');
		return sb.toString();
	}
	
	private static void appendKV(String key, Object value, StringBuilder sb, int depth){
		sb.append('\"');
        if(key == null) sb.append("null");
        else escape(key, sb);
		sb.append('\"').append(':'); 
		sb.append(toJson(value, depth));  
	}
	
	private static String listToJson(List<Object> list, int depth) {
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
			sb.append(toJson(value, depth));
		}
        sb.append(']');
		return sb.toString();
	}
	
	/** 
	 * quotes, \, /, \r, \n, \b, \f, \t, (U+0000 through U+001F)
	 */
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
	
	public static String toJson(Object value) {
		return toJson(value, maxDepth);
	}
	
	@SuppressWarnings("unchecked")
	public static String toJson(Object value, int depth) {
		if(value == null || (depth--) < 0)
			return "null";
		
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
				return "\"" + new SimpleDateFormat(tsPattern).format(value) + "\"";
			if (value instanceof java.sql.Time)
				return "\"" + value.toString() + "\"";
			return "\"" + new SimpleDateFormat(datePattern).format(value) + "\"";
		}
		
		if(value instanceof Map) {
			return mapToJson((Map<Object,Object>)value, depth);
		}
		
		if(value instanceof List) {
			return listToJson((List<Object>)value, depth);
		}
		
		String result = otherToJson(value, depth);
		if (result != null)
			return result;
		
		//default to String
		return "\"" + escape(value.toString()) + "\"";
	}
	
	private static String otherToJson(Object value, int depth) {
		if (value instanceof Character) {
			return "\"" + escape(value.toString()) + "\"";
		} 
		if (value instanceof Object[]) {
			Object[] arr = (Object[])value;
			List<Object> list = new ArrayList<Object>(arr.length);
			for (int i=0; i<arr.length; i++)
				list.add(arr[i]);
			return listToJson(list, depth);
		}
		if (value instanceof Enum) {
			return "\"" + ((Enum<?>)value).toString() + "\"";
		}
		
		return beanToJson(value, depth);
	}
	
	private static String beanToJson(Object model, int depth) {
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
		return mapToJson(map, depth);
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
}





