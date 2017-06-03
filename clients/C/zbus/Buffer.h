#ifndef __ZBUS_BUFFER_H__
#define __ZBUS_BUFFER_H__   

#include <exception>
#include <string> 
using namespace std;
class Buffer {
private:
	int mark = -1;
	int position = 0;
	int limit;
	int capacity;
	int ownData = 0;
	char* data = 0;

public: 
	Buffer(int capacity) {
		this->limit = this->capacity = capacity;

		ownData = 1;
		data = new char[capacity]; 
	}

	Buffer(char* array, int len) {
		ownData = 0;
		data = array;
		limit = capacity = len;
	}

	Buffer(Buffer* buf) {
		this->capacity = buf->capacity;
		this->data = buf->data;
		this->position = buf->position;
		this->limit = buf->limit;
		this->mark = buf->mark;
		this->ownData = 0;
	} 

	~Buffer() {
		if (ownData) {
			delete data;
			data = 0;
		}
	}  

	void mark_() {
		this->mark = this->position;
	}

	Buffer* flip() {
		this->limit = this->position;
		this->position = 0;
		this->mark = -1;
		return this;
	}

	void reset() {
		int m = this->mark;
		if (m < 0) {
			throw new std::exception("mark not set, reset discard"); 
		}
		this->position = m;
	}  
	int remaining() {
		return this->limit - this->position;
	}

	char* begin() { 
		return this->data + this->position;
	}
	char* end() { 
		return this->data + this->limit;
	} 

	Buffer* limit_(int newLimit) {
		if (newLimit>this->capacity || newLimit<0) {
			throw new exception("set new limit error, discarding"); 
		}
		this->limit = newLimit;
		if (this->position > this->limit) this->position = this->limit;
		if (this->mark > this->limit) this->mark = -1;
		return this;
	}

	int drain(int n) {
		if (n <= 0) return 0;

		int res = n;
		int newPos = this->position + n;
		if (newPos>this->limit) {
			newPos = this->limit;
			res = newPos - this->position;
		}
		this->position = newPos;
		if (this->mark > this->position) this->mark = -1;
		return res;
	}

	int copyout(char data[], int len) {
		if (remaining()<len) {
			return -1;
		}
		memcpy(data, this->begin(), len);
		return len;
	}

	int get(char data[], int len) {
		int copyLen = copyout(data, len);
		if (copyLen > 0) {
			drain(len);
		}
		return copyLen;
	}  

	int put(void* data, int len) {
		expandIfNeeded(len);
		memcpy(this->begin(), data, len);
		drain(len);
		return len;
	}

	int put(Buffer* buf) {
		return put(buf->begin(), buf->remaining());
	}

	int putstr(char* str) {
		return put(str, strlen(str));
	}

	int putkv(char* key, char* val) {
		int len = 0;
		len += putstr(key);
		len += putstr(": ");
		len += putstr(val);
		len += putstr("\r\n");
		return len;
	} 

private:
	int expandIfNeeded(int need) {
		if (this->ownData == 0) return -1; //can not expand for duplicated

		int new_cap = this->capacity;
		int new_size = this->position + need;
		char* new_data;
		
		while (new_size > new_cap) {
			new_cap *= 2;
		}
		if (new_cap == this->capacity) return 0;//nothing changed

		new_data = new char[new_cap];
		memcpy(new_data, this->data, this->capacity);
		delete this->data;
		this->data = new_data;
		this->capacity = new_cap;
		this->limit = new_cap;
		return 1;
	}
};


#endif