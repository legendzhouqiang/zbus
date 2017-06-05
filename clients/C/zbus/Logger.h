#ifndef __ZBUS_LOG_H__
#define __ZBUS_LOG_H__

#include "Platform.h" 
#include <cstdio> 
 
#define	LOG_ERROR	0	/* error conditions */
#define	LOG_WARN	1	/* warning conditions */ 
#define	LOG_INFO	2	/* informational */
#define	LOG_DEBUG	3	/* debug-level messages */ 
 
class ZBUS_API Logger {
private: 
	char  logDir[256];
	FILE* logFile;
	int level;
	void* mutex;
	int logDate;

private:
	FILE* getLogFile();
	void createLogFile();
	
public:
	Logger(char* logDir=NULL);
	~Logger();

	void logHead(const int level = LOG_INFO);
	void logBody(void* data,int len, const int level = LOG_INFO);
	
	void debug(const char *format, ...);
	void info(const char *format, ...);
	void warn(const char *format, ...);
	void error(const char *format, ...);
	

	void debug(void* data, int len);
	void info(void* data, int len);
	void warn(void* data, int len);
	void error(void* data, int len);

	void setLevel(int level) {
		this->level = level;
	}
	int getLevel() {
		return this->level;
	}

	bool isDebugEnabled() {
		return level >= LOG_DEBUG;
	}

private:
	static Logger defaultLogger;
public:
	static void configDefaultLogger(char* logDir = NULL, int level=LOG_INFO);
	static Logger* getLogger();
};

#endif

