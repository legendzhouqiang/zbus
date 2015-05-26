package org.zstacks.zbus.rpc;

import java.util.Arrays;
import java.util.Random;

import org.zstacks.zbus.client.Broker;
import org.zstacks.zbus.client.broker.SingleBroker;
import org.zstacks.zbus.client.broker.SingleBrokerConfig;
import org.zstacks.zbus.client.rpc.RpcProxy;
import org.zstacks.zbus.rpc.biz.Interface;
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

	public static void main(String[] args) throws Exception {
		// 1）创建Broker代表
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		RpcProxy proxy = new RpcProxy(broker); 
		
		Interface hello = proxy.getService(Interface.class, "mq=MyRpc");

		Object[] res = hello.objectArray();
		for (Object obj : res) {
			System.out.println(obj);
		}

		Object[] array = new Object[] { getUser("rushmore"), "hong", true, 1,
				String.class };
		
		
		int saved = hello.saveObjectArray(array);
		System.out.println(saved);
		 
		Class<?> ret = hello.classTest(String.class);
		System.out.println(ret);
		
		
		broker.close();
	}
}
