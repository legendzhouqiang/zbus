#ifndef __ZBUS_MESSAGE_H__
#define __ZBUS_MESSAGE_H__   

#include <string>
#include <map>
#include <vector>
using namespace std;

class Message {
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