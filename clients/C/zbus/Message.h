#ifndef __ZBUS_MESSAGE_H__
#define __ZBUS_MESSAGE_H__   

#include "Platform.h"
#include "Protocol.h"
#include "Buffer.h"
#include "Kit.h"

#include <string>
#include <cstring> 
#include <map>  

using namespace std;

ZBUS_API class Message {
public:
	string getCmd() {
		return getHeader(HEADER_CMD);
	}
	void setCmd(string value) {
		setHeader(HEADER_CMD, value);
	}  
	string getTopic() {
		return getHeader(HEADER_TOPIC);
	}
	void setTopic(string value) {
		setHeader(HEADER_TOPIC, value);
	}
	string getConsumeGroup() {
		return getHeader(HEADER_CONSUME_GROUP);
	}
	void setConsumeGroup(string value) {
		setHeader(HEADER_CONSUME_GROUP, value);
	}
	string getSender() {
		return getHeader(HEADER_SENDER);
	}
	void setSender(string value) {
		setHeader(HEADER_SENDER, value);
	}
	string getRecver() {
		return getHeader(HEADER_RECVER);
	}
	void setRecver(string value) {
		setHeader(HEADER_RECVER, value);
	}
	string getToken() {
		return getHeader(HEADER_TOKEN);
	}
	void setToken(string value) {
		setHeader(HEADER_TOKEN, value);
	}
	string getId() {
		return getHeader(HEADER_ID);
	}
	void setId(string value) {
		setHeader(HEADER_ID, value);
	}
	string getOriginId() {
		return getHeader(HEADER_ORIGIN_ID);
	}
	void setOriginId(string value) {
		setHeader(HEADER_ORIGIN_ID, value);
	}
	string getOriginStatus() {
		return getHeader(HEADER_ORIGIN_STATUS);
	}
	void setOriginStatus(string value) {
		setHeader(HEADER_ORIGIN_STATUS, value);
	}
	string getOriginUrl() {
		return getHeader(HEADER_ORIGIN_URL);
	}
	void setOriginUrl(string value) {
		setHeader(HEADER_ORIGIN_URL, value);
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
		printf("\n");
	}

	void encode(ByteBuffer& buf) { 
		if (status != "") {
			string desc = HttpStatusTable()[status];
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

    inline static Message* decode(ByteBuffer& buf) {
		buf.mark();

		Message* msg = new Message(); 

		int len = findHeadLength(buf);
		if (len < 0) return false;
		char* headerStr = new char[len + 1];
		strncpy(headerStr, buf.begin(), len);
		headerStr[len] = '\0';
		bool ok = parseHead(headerStr, msg);
		delete headerStr;
		if (!ok) {
			buf.reset();
			delete msg;
			return NULL;
		}

		buf.drain(len);
		string contentLen = msg->header["content-length"];
		if (contentLen == "") {//no content-length, treat as message without body
			return msg;
		} 
		int nBody = atoi(contentLen.c_str());
		if (nBody > buf.remaining()) {
			buf.reset();
			delete msg;
			return NULL;
		}  

		msg->body = new unsigned char[nBody];
		msg->bodyLength = nBody;
		buf.get((char*)msg->body, nBody);  
		return msg;
	}

private:
	inline static int findHeadLength(ByteBuffer& buf) {
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

    inline static bool parseHead(char* buf, Message* msg) {   
		char* headCtx;
		char* p = strtok_s(buf, "\r\n", &headCtx);
		if (!p) { 
			return false;
		} 

		char* metaCtx;
		char* m = strtok_s(p, " ", &metaCtx);
		if (cmpIgnoreCase(m, "HTTP")) {
			msg->status = strtok_s(NULL, " ", &metaCtx);
		} else {
			msg->url = strtok_s(NULL, " ", &metaCtx);
		}

		p = strtok_s(NULL, "\r\n", &headCtx);
		while (p) {
			char* d = strchr(p, ':');
			if (d) {//ignore not key value
				char* key = strdupTrimed(p, d - p);
				char* val = strdupTrimed(d + 1, p + strlen(p) - d - 1);
				msg->header[string(key)] = string(val);
				free(key);
				free(val);
			}
			p = strtok_s(NULL, "\r\n", &headCtx);
		}
		return true;
	}  

	static map<string, string>& HttpStatusTable() {
		static bool inited = false;
		static map<string, string> table;
		if (!inited) {

			table["200"] = "OK";
			table["201"] = "Created";
			table["202"] = "Accepted";
			table["204"] = "No Content";
			table["206"] = "Partial Content";
			table["301"] = "Moved Permanently";
			table["304"] = "Not Modified";
			table["400"] = "Bad Request";
			table["401"] = "Unauthorized";
			table["403"] = "Forbidden";
			table["404"] = "Not Found";
			table["405"] = "Method Not Allowed";
			table["416"] = "Requested Range Not Satisfiable";
			table["500"] = "Internal Server Error";
		}
		return table;
	}
};
 


#endif