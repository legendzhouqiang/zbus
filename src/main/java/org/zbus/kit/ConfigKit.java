package org.zbus.kit;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ConfigKit {    
	
	public static String option(Properties props, String opt, String defaultValue){
		String value = props.getProperty(opt, defaultValue);
		return value == null? null : value.trim();
	}
	
	public static int option(Properties props, String opt, int defaultValue){
		String value = option(props, opt, null);
		if(value == null) return defaultValue;
		return Integer.valueOf(value);
	}
	
	public static String option(String[] args, String opt, String defaultValue){
		for(int i=0; i<args.length;i++){
			if(args[i].equals(opt)){
				if(i<args.length-1) return args[i+1];
			} 
		}
		return defaultValue;
	}
	
	public static int option(String[] args, String opt, int defaultValue){
		String value = option(args, opt, null);
		if(value == null) return defaultValue;
		return Integer.valueOf(value);
	}
	
	public static boolean option(String[] args, String opt, boolean defaultValue){
		String value = option(args, opt, null);
		if(value == null) return defaultValue;
		return Boolean.valueOf(value);
	}
	
	private static Set<String> split(String value){
		Set<String> res = new HashSet<String>();
		String[] blocks = value.split("[,]");
		for(String b : blocks){
			b = b.trim();
			if("".equals(b)) continue;
			res.add(b);
		}
		return res;
	} 
	
	public static String value(Properties props, String name, String defaultValue){ 
		return props.getProperty(name, defaultValue).trim();
	}
	public static int value(Properties props, String name, int defaultValue){ 
		String value = value(props, name, "");
		if("".equals(value)) return defaultValue;
		return Integer.valueOf(value);
	}
	
	public static boolean value(Properties props, String name, boolean defaultValue){ 
		String value = value(props, name, "");
		if("".equals(value)) return defaultValue;
		return Boolean.valueOf(value);
	}
	public static String value(Properties props, String name){
		return value(props, name, "");
	}
	public static Set<String> valueSet(Properties props, String name){ 
		return split(value(props, name));
	} 

	public static Properties loadConfig(String fileName){ 
		Properties props = new Properties();
		try{
			InputStream fis = FileKit.loadFile(fileName);
			if(fis != null){
				props.load(fis);
			}
		} catch(Exception e){ 
			System.out.println("Missing config, using default empty");
		}
		return props;
	}
}
