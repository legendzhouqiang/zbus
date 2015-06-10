package org.zstacks.zbus.rpc.biz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
 
/**
 * 私有异常，测试前端未知场景
 * @author 洪磊明
 *
 */
class PrivateRuntimeException extends RuntimeException{  
	private static final long serialVersionUID = 4587336984841564800L;

	public PrivateRuntimeException() {
		super(); 
	}

	public PrivateRuntimeException(String message, Throwable cause) {
		super(message, cause); 
	}

	public PrivateRuntimeException(String message) {
		super(message); 
	}

	public PrivateRuntimeException(Throwable cause) {
		super(cause); 
	}
	
}
public class InterfaceImpl implements Interface{
 
	public String getString(String name) {
		return "Hello World ZBUS " + name;
	}

	
	public String[] stringArray() {
		return new String[]{"hong", "leiming"};
	}
	
	public Object[] objectArray(String id){  
		return new Object[]{id, getUser("rushmore"), "hong", true, 1, String.class};
	}
	
	
	public int plus(int a, int b) { 
		return a+b;
	}
	
	
	public User getUser(String name) {
		User user = new User();
		user.setName(name);
		user.setPassword("password"+System.currentTimeMillis());
		user.setAge(new Random().nextInt(100));
		user.setItem("item_1");
		user.setRoles(Arrays.asList("admin", "common"));		
		return user;
	}
	
	
	public Order getOrder() {
		Order order = new Order();
		order.setItem(Arrays.asList("item1","item2","item3"));
		return order;
	}
	
	
	public Object[] getUsers() {
		return new User[]{getUser("hong"), getUser("leiming")};
	}
	
	
	public Map<String, Object> map(int value1) {
		HashMap<String, Object> res = new HashMap<String, Object>();
		res.put("key1", value1);
		res.put("key2", "value2");
		res.put("key3", 2.5);
		return res;
	}
	
	
	public List<Map<String, Object>> listMap() {
		List<Map<String, Object>> res = new ArrayList<Map<String,Object>>();
		res.add(map(1));
		res.add(map(2));
		res.add(map(3));
		return res;
	}
	
	
	public int saveObjectArray(Object[] array) {
		for(Object obj : array){
			System.out.println(obj);
		}
		return 0;
	}
	
	@Override
	public int saveUserArray(User[] array) {
		for(User user : array){
			System.out.println(user);
		}
		return 0;
	}
	
	@Override
	public int saveUserList(List<User> array) {
		for(User user : array){
			System.out.println(user);
		}
		return 0;
	}
	
	public void throwException() {
		throw new RuntimeException("runtime exception from server");
	}
	public void throwNullPointerException(){
		throw new NullPointerException("null pointer");
	}
	
	public void throwUnkownException() {  
		throw new PrivateRuntimeException("private runtime exeption");
	}
	
	
	public void noReturn() {
		System.out.println("called noReturn");
	}
	
	
	public Class<?> classTest(Class<?> inClass) {
		System.out.println(inClass);
		return Double.class;
	}
}