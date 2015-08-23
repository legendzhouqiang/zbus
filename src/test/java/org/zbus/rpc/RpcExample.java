package org.zbus.rpc;

import java.util.Arrays;
import java.util.Random;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.MessageInvoker;
import org.zbus.rpc.biz.Interface;
import org.zbus.rpc.biz.MyEnum;
import org.zbus.rpc.biz.User;
import org.zbus.rpc.broking.BrokingInvoker;

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
	
	public static void test2(Interface hello) throws Exception{ 
		int res = hello.plus(1,2);
		System.out.println(res);
		
		//System.out.println(hello.testEncoding());
	}
	
	public static void test(Interface hello) throws Exception{ 

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
		BrokerConfig brokerConfig = new BrokerConfig();
		brokerConfig.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(brokerConfig);

		MessageInvoker invoker = new BrokingInvoker(broker, "MyRpc");
		
		RpcFactory factory = new RpcFactory(invoker); 
		
		RpcConfig config = new RpcConfig(); 
		config.setVerbose(true);
		config.setTimeout(10000); 
		
		
		Interface hello = factory.getService(Interface.class, config);
		for(int i=0;i<1;i++){ 
			test(hello); 
		}
		
		broker.close();
	}
}
