package io.zbus.unittests;

import java.util.Set;

import io.zbus.kit.ClassKit;
import io.zbus.rpc.Remote;

public class Test {

	public static void main(String[] args) throws Exception {   
		Set<Class<?>> all = ClassKit.scan(Remote.class); 
		
		for(Class<?> c : all){
			System.out.println(c);
		}
		
		System.out.println(all.size());
	}

}
