package org.zbus.rpc;

import java.util.Arrays;
import java.util.Random;

import org.zbus.client.Broker;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;
import org.zbus.client.rpc.Rpc;
import org.zbus.rpc.biz.User;

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
		SingleBrokerConfig config = new SingleBrokerConfig();
		config.setBrokerAddress("127.0.0.1:15555");
		Broker broker = new SingleBroker(config);
		
		Rpc rpc = new Rpc(broker, "MyRpc");
		rpc.setModule("Interface");
		
		String res = (String)rpc.invokeSync("getString", "hong");
		
		System.out.println(res);
		
		broker.close();
	}
}
