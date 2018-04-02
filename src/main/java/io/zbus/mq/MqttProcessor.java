package io.zbus.mq;

import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.zbus.net.Session;

public class MqttProcessor {

	public void processConnect(MqttConnectMessage msg, Session sess) {

	}

	public void processDisconnect(MqttConnectMessage msg, Session sess) {

	}

	public void processPublish(MqttPublishMessage msg, Session sess) {

	}

	public void processPubAck(MqttPubAckMessage msg, Session sess) {

	}

	public void processPubComp(MqttMessage msg, Session sess) {

	}

	public void processPubRec(MqttMessage msg, Session sess) {

	}

	public void processPubRel(MqttMessage msg, Session sess) {

	}

	public void processSubscribe(MqttSubscribeMessage msg, Session sess) {

	}

	public void processUnsubscribe(MqttUnsubscribeMessage msg, Session sess) {

	}
}
