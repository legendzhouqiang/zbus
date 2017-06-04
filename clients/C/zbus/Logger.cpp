#include "Logger.h" 
#include "Thread.h"

#include <cassert>
#include <cstdlib>
#include <ctime>
 
static int file_exists(const char* path){

#if defined (__UNIX__)
	return access(path, 0) == 0;
#elif (defined (__WINDOWS__))
	return _access(path, 0) == 0;
#endif

}
static int check_and_mk_log_dir(const char *base_path){
	if (!file_exists(base_path)){

#if defined (__UNIX__)
		if (mkdir(base_path, 0755) != 0){
#elif (defined (__WINDOWS__))
		if (_mkdir(base_path) != 0){
#endif
			return errno != 0 ? errno : EPERM;
		}
	}
	return 0;
}

static int64_t current_time(void) {
#if defined (__UNIX__)
	struct timeval tv;
	gettimeofday (&tv, NULL);

	return (int64_t) ((int64_t) tv.tv_sec * 1000 + (int64_t) tv.tv_usec / 1000);
#elif (defined (__WINDOWS__))
	FILETIME ft;
	GetSystemTimeAsFileTime (&ft);
	return (int64_t) (*((int64_t *) (&ft)) / 10000);
#endif
}

 

Logger::Logger(char* logDir) {
	this->level = LOG_INFO;
	if (logDir == NULL) {
		this->log_file = stdout;
	}
	else {
		strcpy(this->log_dir, log_dir);
		check_and_mk_log_dir(this->log_dir);
	}
	this->mutex = (pthread_mutex_t*)malloc(sizeof(pthread_mutex_t));
	pthread_mutex_init((pthread_mutex_t*)this->mutex, 0);
}	

Logger::~Logger(){
	if (this->log_file != stdout) {
		fclose(this->log_file);
	}
	if (this->mutex) {
		pthread_mutex_destroy((pthread_mutex_t*)this->mutex);
		this->mutex = 0; 
	}
}

#define _DO_LOG(level) do{\
	if(this->level >= (level)){\
		pthread_mutex_lock((pthread_mutex_t*)this->mutex);\
		this->logHead((level));\
		FILE* file = this->logFile();\
		va_list argptr;\
		va_start (argptr, format);\
		vfprintf ((file), format, argptr);\
		va_end (argptr);\
		fprintf (file, "\n");\
		fflush (file);\
		pthread_mutex_unlock((pthread_mutex_t*)this->mutex);\
	}\
}while(0)

void Logger::info(const char *format, ...) {
	int level = LOG_INFO;
	_DO_LOG(level);
}

void Logger::debug(const char *format, ...) {
	int level = LOG_DEBUG;
	_DO_LOG(level);
}

void Logger::warn(const char *format, ...) {
	int level = LOG_WARN;
	_DO_LOG(level);
}

void Logger::error(const char *format, ...) {
	int level = LOG_ERROR;
	_DO_LOG(level);
}

FILE* Logger::logFile() { 
	int date;
	char fdate[32];
	if (this->log_file == stdout) {
		return this->log_file;
	} 
	time_t curtime = time(NULL);
	struct tm *loctime;
	loctime = localtime(&curtime);
	strftime(fdate, 32, "%Y%m%d", loctime);
	date = atoi(fdate);

	if (date > this->log_date) {
		char newfile[256];
		this->log_date = date;
		snprintf(newfile, sizeof(newfile), "%s/%s.log", this->log_dir, fdate);
		if (this->log_file) {
			fclose(this->log_file);
		}
		this->log_file = fopen(newfile, "a+");
		if (!this->log_file) {
			printf("create log file error[%s],using stdout instead\n", newfile);
			this->log_file = stdout;
		}
	}
	return this->log_file; 
}


void Logger::logHead(const int priority) {
	FILE* file;
	time_t curtime = time(NULL);
	struct tm *loctime;
	char formatted[32];
	char *caption;

	loctime = localtime(&curtime);
	file = logFile();
	strftime(formatted, 32, "[%Y-%m-%d %H:%M:%S", loctime);
	fprintf(file, "%s.%03d] ", formatted, current_time() % 1000);

	switch (priority)
	{
	case LOG_DEBUG:
		caption = "DEBUG";
		break;
	case LOG_INFO:
		caption = "INFO";
		break;
		break;
	case LOG_WARN:
		caption = "WARNING";
		break;
	case LOG_ERROR:
		caption = "ERROR";
		break;
	default:
		caption = "UNKOWN";
		break;
	}

	fprintf(file, "%s - ", caption);
}