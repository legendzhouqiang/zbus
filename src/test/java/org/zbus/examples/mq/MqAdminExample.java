package org.zbus.examples.mq;

import org.zbus.broker.Broker;
import org.zbus.broker.SingleBroker;
import org.zbus.mq.MqAdmin;

public class MqAdminExample {
	public static void main(String[] args) throws Exception {
		Broker broker = new SingleBroker(); // default to 127.0.0.1:15555

		MqAdmin admin = new MqAdmin(broker, "MyMQ");
		admin.removeMQ();

		broker.close();
	}
}
