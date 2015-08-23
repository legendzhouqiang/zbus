package org.zbus.rpc;

import java.util.Arrays;
import java.util.Random;

import org.zbus.mq.Broker;
import org.zbus.mq.BrokerConfig;
import org.zbus.mq.SingleBroker;
import org.zbus.net.http.MessageInvoker;
import org.zbus.rpc.biz.User;
import org.zbus.rpc.broking.BrokingInvoker;

public class RpcRawExample {
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
		BrokerConfig config = new BrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
	
		MessageInvoker invoker = new BrokingInvoker(broker, "MyRpc");
		
		RpcInvoker rpc = new RpcInvoker(invoker);   
		
		String res = rpc.invokeSync(String.class, "getString", "test");
		
		System.out.println(res);
		
		broker.close();
	}
}
