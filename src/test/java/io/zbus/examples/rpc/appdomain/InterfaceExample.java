package io.zbus.examples.rpc.appdomain;

import java.util.List;
import java.util.Map;

/**
 * 测试ZBUS RPC的透明度
 * 1) 接口类无任何ZBUS相关侵入
 * 2）覆盖各种复杂的入参与出参，异常, 目标是跟本地方法一样无任何限制
 * 
 * @author 洪磊明 rushmore
 *
 */
public interface InterfaceExample{
  
	int getUserScore();
	
	String echo(String string);
	
	String getString(String name); 
	//test of method overloading（方法重载）
	String getString(String name, int c);
	
	String[] stringArray();
	
	byte[] getBin();
	
	int plus(int a, int b);
	
	MyEnum myEnum(MyEnum e);
	
	User getUser(String name);
	
	Order getOrder();
	
	User[] getUsers();
	
	Object[] objectArray(String id);

	int saveObjectArray(Object[] array);
	
	int saveUserArray(User[] array);
	
	int saveUserList(List<User> array);
	

	Map<String, Object> map(int value1);
	
	List<Map<String, Object>> listMap();
	
	String testEncoding();
	
	Class<?> classTest(Class<?> inClass);
	
	void testTimeout();
	
	void noReturn();
	
	void throwNullPointerException();
	
	void throwException();
	
	void throwUnkownException();
}