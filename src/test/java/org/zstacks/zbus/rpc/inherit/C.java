package org.zstacks.zbus.rpc.inherit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
 


public class C extends B{
	public String echo(String value){
		return value;
	}
	
	@Override
	public String getString() { 
		return "c's string";
	}
	
	public static List<Class<?>> getAllInterfaces(Class<?> clazz){
		List<Class<?>> res = new ArrayList<Class<?>>();
		while(clazz != null){ 
			res.addAll(Arrays.asList(clazz.getInterfaces()));
			clazz = clazz.getSuperclass();
		}
		return res;
	}
	
	public static void main(String[] args) throws Exception { 
		
		for(Class<?> i : getAllInterfaces(C.class)){
			System.out.println(i);
		}
	}
}
