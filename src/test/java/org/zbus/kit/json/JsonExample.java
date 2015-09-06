package org.zbus.kit.json;

import org.zbus.rpc.biz.User;

public class JsonExample {

	public static void main(String[] args) { 
		User user = new User();
		user.setName("hong");
		
		String res = Json.toJSONString(user);
		System.out.println(res);
	} 
}
