#ifndef __ZBUS_MESSAGE_H__
#define __ZBUS_MESSAGE_H__   

#include "Protocol.h"
#include "Buffer.h"
#include "Kit.h"

#include <string>
#include <cstring> 
#include <map>  

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
	void* body = 0;
	int bodyLength = 0;
public:
	string status; //should be integer
	string method = "GET";
	string url = "/";
	map<string, string> header;  
	

public:
	~Message() {
		if (this->body) {
			delete this->body;
			this->body = 0;
			this->bodyLength = 0;
		}
	}

	void setBody(string& body) {
		setBody((void*) body.c_str(), body.size());
	}
	void setBody(void* body, int bodyLength) {
		if (this->body) {
			delete this->body;
		} 
		this->bodyLength = bodyLength;
		this->body = new char[this->bodyLength+1];
		memcpy(this->body, body, bodyLength);
		((char*)this->body)[this->bodyLength] = 0; // make it char* compatible
	}

	void setBody(char* body) {
		setBody(body, strlen(body));
	}

	void setJsonBody(char* body) {
		header["content-type"] = "application/json";
		setBody(body);
	}   

	char* getBodyString() {
		return (char*)body;
	} 

	void print() {
		ByteBuffer buf;
		encode(buf);
		buf.flip();
		buf.print();
	}

	void encode(ByteBuffer& buf) { 
		if (status != "") {
			string desc = HttpStatus::Table[status];
			if (desc == "") {
				desc = "Unknown Status";
			} 
			char data[256];
			snprintf(data, sizeof(data), "HTTP/1.1 %s %s\r\n", status.c_str(), desc.c_str()); 
			buf.put(data);
		} else { 
			char data[256];
			snprintf(data, sizeof(data), "%s %s HTTP/1.1\r\n", method.c_str(), url.c_str()); 
			buf.put(data);
		}

		for (map<string, string>::iterator iter = header.begin(); iter != header.end(); iter++) {
			string key = iter->first;
			string val = iter->second;
			if (key == "content-length") continue;
			buf.putKeyValue((char*)key.c_str(), (char*)val.c_str()); 
		} 
		 
		char len[100];
		snprintf(len, sizeof(len), "%d", bodyLength);
		buf.putKeyValue("content-length", len);

		buf.put("\r\n");
		if (bodyLength > 0) {
			buf.put(body, bodyLength);
		} 
	}  

	bool decode(ByteBuffer& buf) {
		buf.mark();

		//clear message
		this->header.clear(); 
		if (this->body) { 
			delete this->body;
			this->body = NULL;
		}
		this->bodyLength = 0;
		this->status = "";

		int len = findHeadLength(buf);
		if (len < 0) return false;
		char* headerStr = new char[len + 1];
		strncpy(headerStr, buf.begin(), len);
		headerStr[len] = '\0';
		bool ok = parseHead(headerStr);
		delete headerStr;
		if (!ok) {
			buf.reset();
			return false;
		}

		buf.drain(len);
		string contentLen = header["content-length"];
		if (contentLen == "") {//no content-length, treat as message without body
			return true;
		} 
		int nBody = atoi(contentLen.c_str());
		if (nBody > buf.remaining()) {
			buf.reset();
			return false;
		}  

		this->body = new unsigned char[nBody];
		this->bodyLength = nBody;
		buf.get((char*)this->body, nBody);  
		return true;
	}

private:
	int findHeadLength(ByteBuffer& buf) {
		char* begin = buf.begin();
		char* p = begin;
		char* end = buf.end();
		while (p + 3 < end) {
			if (*(p + 0) == '\r' && *(p + 1) == '\n' && *(p + 2) == '\r' && *(p + 3) == '\n') {
				return p + 4 - begin;
			}
			p++;
		}
		return -1;
	}

	bool parseHead(char* buf) {   
		char* headCtx;
		char* p = strtok_s(buf, "\r\n", &headCtx);
		if (!p) { 
			return false;
		} 

		char* metaCtx;
		char* m = strtok_s(p, " ", &metaCtx);
		if (cmpIgnoreCase(m, "HTTP")) {
			status = strtok_s(NULL, " ", &metaCtx);
		} else {
			url = strtok_s(NULL, " ", &metaCtx);
		}

		p = strtok_s(NULL, "\r\n", &headCtx);
		while (p) {
			char* d = strchr(p, ':');
			if (d) {//ignore not key value
				char* key = strdupTrimed(p, d - p);
				char* val = strdupTrimed(d + 1, p + strlen(p) - d - 1);
				header[string(key)] = string(val);
				free(key);
				free(val);
			}
			p = strtok_s(NULL, "\r\n", &headCtx);
		}
		return true;
	} 
};
 


#endif