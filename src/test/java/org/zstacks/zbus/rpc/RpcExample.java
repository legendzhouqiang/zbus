package org.zstacks.zbus.rpc;

import java.util.Arrays;
import java.util.Random;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.rpc.RpcProxy;
import org.zstacks.zbus.rpc.biz.Interface;
import org.zstacks.zbus.rpc.biz.MyEnum;
import org.zstacks.zbus.rpc.biz.User;

public class RpcExample {
	public static User getUser(String name) {
		User user = new User();
		user.setName(name);
		user.setPassword("password" + System.currentTimeMillis());
		user.setAge(new Random().nextInt(100));
		user.setItem("item_1");
		user.setRoles(Arrays.asList("admin", "common"));
		return user;
	}
	
	public static void test(RpcProxy proxy) throws Exception{
		
		
		Interface hello = proxy.getService(Interface.class, "mq=MyRpc");
/*
		Object[] res = hello.objectArray("xzx");
		for (Object obj : res) {
			System.out.println(obj);
		}

		Object[] array = new Object[] { getUser("rushmore"), "hong", true, 1,
				String.class };
		
		
		int saved = hello.saveObjectArray(array);
		System.out.println(saved);
		 
		Class<?> ret = hello.classTest(String.class);
		System.out.println(ret);
		
		User[] users = new User[]{ getUser("rushmore"),  getUser("rushmore2")};
		hello.saveUserArray(users);
		
		hello.saveUserList(Arrays.asList(users));
		
		Object[] objects = hello.getUsers();
		for(Object obj : objects){
			System.out.println(obj);
		}
		*/
		MyEnum e = hello.myEnum(MyEnum.Monday);
		System.out.println(e);
	}
	

	public static void main(String[] args) throws Exception { 
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);

		RpcProxy proxy = new RpcProxy(broker); 
		test(proxy);
		
		broker.close();
	}
}
