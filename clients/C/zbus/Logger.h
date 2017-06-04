#ifndef __ZBUS_LOG_H__
#define __ZBUS_LOG_H__

#include "Platform.h" 
#include <cstdio> 
 
#define	LOG_ERROR	0	/* error conditions */
#define	LOG_WARN	1	/* warning conditions */ 
#define	LOG_INFO	2	/* informational */
#define	LOG_DEBUG	3	/* debug-level messages */ 
 
class Logger {
private: 
	char  log_dir[256];
	FILE* log_file;
	int level;
	void* mutex;
	int log_date;

private:
	FILE* logFile();
	void logHead(const int priority = LOG_INFO);
public:
	Logger(char* logDir=NULL);
	~Logger();

	void info(const char *format, ...);
	void debug(const char *format, ...);
	void error(const char *format, ...);
	void warn(const char *format, ...); 

	void setLevel(int level) {
		this->level = level;
	}
	int getLevel() {
		return this->level;
	}
};

#endif

