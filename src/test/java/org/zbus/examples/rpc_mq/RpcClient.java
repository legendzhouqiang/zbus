package org.zbus.examples.rpc_mq;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.zbus.broker.Broker;
import org.zbus.broker.BrokerConfig;
import org.zbus.broker.SingleBroker;
import org.zbus.examples.rpc_biz.Interface;
import org.zbus.examples.rpc_biz.MyEnum;
import org.zbus.examples.rpc_biz.User;
import org.zbus.net.http.Message.MessageInvoker;
import org.zbus.rpc.RpcFactory;
import org.zbus.rpc.mq.MqInvoker;

public class RpcClient {
	public static User getUser(String name) {
		User user = new User();
		user.setName(name);
		user.setPassword("password" + System.currentTimeMillis());
		user.setAge(new Random().nextInt(100));
		user.setItem("item_1");
		user.setRoles(Arrays.asList("admin", "common"));
		return user;
	}
	 
	public static void test(Interface hello) throws Exception{ 

		List<Map<String, Object>> list = hello.listMap();
		System.out.println(list);
		
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
		
		MyEnum e = hello.myEnum(MyEnum.Monday);
		System.out.println(e);
	}
	
	public static void main(String[] args) throws Exception { 
		//1)创建Broker代表（可用高可用替代）
		BrokerConfig config = new BrokerConfig();
		config.setServerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		 
		//2)创建基于MQ的Invoker以及Rpc工厂，指定RPC采用的MQ为MyRpc
		MessageInvoker invoker = new MqInvoker(broker, "MyRpc");   
		RpcFactory factory = new RpcFactory(invoker); 
		
		//3) 动态代理出实现类
		Interface hello = factory.getService(Interface.class);
		
		test(hello);  
		
		broker.close();
	}  
}
