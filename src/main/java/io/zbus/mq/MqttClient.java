package io.zbus.mq;

import java.util.LinkedList;
import java.util.List;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.zbus.net.Client;
import io.zbus.net.EventLoop;

public class MqttClient extends Client<MqttMessage, MqttMessage> {

	public MqttClient(String address, EventLoop loop) {
		super(address, loop);

		codec(p -> {
			p.add(new MqttDecoder());
			p.add(MqttEncoder.INSTANCE);
		});
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		MqttConnectMessage message = MqttMessageBuilders.connect()
				.clientId("hong")
				.protocolVersion(MqttVersion.MQTT_3_1_1)
                .username("admin")
                .password("public".getBytes())
                .willRetain(true)
                .willQoS(MqttQoS.AT_LEAST_ONCE)
                .willFlag(true)
                .willTopic("/my_will")
                .willMessage("gone".getBytes())
                .cleanSession(true)
                .keepAlive(600)
                .build();
		
		EventLoop loop = new EventLoop();
		MqttClient client = new MqttClient("localhost:1883", loop);
		client.onOpen = ()->{
			client.sendMessage(message);
			client.sendMessage(createSubMessage());
		};
		client.onMessage = msg->{
			System.out.println(msg);
			
		};
		client.connect();
	}
	
	static MqttSubscribeMessage createSubMessage() {
		MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE,
				true, 0);
		MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader.from(12345);

		List<MqttTopicSubscription> topicSubscriptions = new LinkedList<MqttTopicSubscription>();
		topicSubscriptions.add(new MqttTopicSubscription("/abc", MqttQoS.AT_LEAST_ONCE));
		topicSubscriptions.add(new MqttTopicSubscription("/def", MqttQoS.AT_LEAST_ONCE));
		topicSubscriptions.add(new MqttTopicSubscription("/xyz", MqttQoS.EXACTLY_ONCE));

		MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(topicSubscriptions);
		return new MqttSubscribeMessage(mqttFixedHeader, mqttMessageIdVariableHeader, mqttSubscribePayload);
	}
}
