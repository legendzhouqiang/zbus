#ifndef __ZBUS_MESSAGE_H__
#define __ZBUS_MESSAGE_H__   

#include "Protocol.h"

#include <string>
#include <map>
#include <vector>
using namespace std;

class Message {
public:
	string getCmd() {
		return getHeader(Protocol::CMD);
	}
	void setCmd(string value) {
		setHeader(Protocol::CMD, value);
	}  
	string getTopic() {
		return getHeader(Protocol::TOPIC);
	}
	void setTopic(string value) {
		setHeader(Protocol::TOPIC, value);
	}
	string getConsumeGroup() {
		return getHeader(Protocol::CONSUME_GROUP);
	}
	void setConsumeGroup(string value) {
		setHeader(Protocol::CONSUME_GROUP, value);
	}
	string getSender() {
		return getHeader(Protocol::SENDER);
	}
	void setSender(string value) {
		setHeader(Protocol::SENDER, value);
	}
	string getRecver() {
		return getHeader(Protocol::RECVER);
	}
	void setRecver(string value) {
		setHeader(Protocol::RECVER, value);
	}
	string getToken() {
		return getHeader(Protocol::TOKEN);
	}
	void setToken(string value) {
		setHeader(Protocol::TOKEN, value);
	}
	string getId() {
		return getHeader(Protocol::ID);
	}
	void setId(string value) {
		setHeader(Protocol::ID, value);
	}
	string getOriginId() {
		return getHeader(Protocol::ORIGIN_ID);
	}
	void setOriginId(string value) {
		setHeader(Protocol::ORIGIN_ID, value);
	}
	string getOriginStatus() {
		return getHeader(Protocol::ORIGIN_STATUS);
	}
	void setOriginStatus(string value) {
		setHeader(Protocol::ORIGIN_STATUS, value);
	}
	string getOriginUrl() {
		return getHeader(Protocol::ORIGIN_URL);
	}
	void setOriginUrl(string value) {
		setHeader(Protocol::ORIGIN_URL, value);
	}

	string getHeader(string key, string defaultValue = "") {
		string res = header[key];  
		if (res == "") return defaultValue;
		return res;
	}

	void setHeader(string key, string value) {
		header[key] = value;
	}

private:
	vector<unsigned char> body;   
public:
	string status; //should be integer
	string method = "GET";
	string url = "/";
	map<string, string> header; 

	void setBody(string& body);
	void setBody(void* body, int64_t bodyLength);
	void setBody(char* body);
	void setJsonBody(char* body);
};
 


#endif