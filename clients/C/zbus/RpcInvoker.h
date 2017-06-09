#ifndef __ZBUS_RPCINVOKER_H__
#define __ZBUS_RPCINVOKER_H__  
 
#include "Producer.h"
#include <json/value.h>
#include <json/reader.h>
#include <json/writer.h>

class ZBUS_API Request { 
public:
	std::string method;
	std::string module;
	std::vector<Json::Value> params; 
};

class ZBUS_API Response {
public:
	Json::Value result;
	Json::Value error;

	bool isError() {
		return !error.isNull();
	}
};

class ZBUS_API RpcInvoker {
public:
	RpcInvoker(Broker* broker, std::string topic, int timeout = 3000, ServerSelector selector = NULL) :
		producer(broker){
		this->broker = broker;
		this->topic = topic;
		this->timeout = timeout;
		this->rpcSelector = selector; 
	}
	
	Response invoke(Request& req, int timeout = 3000, ServerSelector selector = NULL) {
		Message msg;
		msg.setAck(false);
		msg.setTopic(topic);
		msg.setToken(token); 

		Json::Value root;
		root["method"] = req.method;
		root["module"] = req.module;
		Json::Value params;
		for (Json::Value& param : req.params) {
			params.append(param);
		}
		root["params"] = params;

		Json::StyledWriter writer;  
		msg.setJsonBody(writer.write(root)); 
		
		if (selector == NULL) {
			selector = rpcSelector;
		}
		Message msgRes = producer.produce(msg, timeout, selector);
		Response res;
		std::string bodyString = msgRes.getBodyString();
		if (msgRes.status != "200") {
			res.error = Json::Value(bodyString);
		}
		else {
			Json::Reader reader;
			Json::Value value;
			reader.parse(bodyString, value);
			res.error = value["error"];
			res.result = value["result"];
		}
		return res;
	}
	
private:
	Broker* broker;
	std::string topic;
	std::string token;
	int timeout;
	ServerSelector rpcSelector;
	Producer producer;
};



#endif
