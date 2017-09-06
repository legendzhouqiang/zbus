
package io.zbus.kit;

public class ClassKit {
	
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(String className) throws Exception{ 
		Class<?> clazz = Class.forName(className);
		return (T)clazz.newInstance(); 
	}
}
