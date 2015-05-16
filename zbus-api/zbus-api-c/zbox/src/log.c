#include "log.h" 
#include "thread.h"
static int file_exists(const char* path);
static int check_and_mk_log_dir(const char *base_path);
static int64_t current_time(void);

struct _zlog{
	char  log_dir[256];
	FILE* log_file;
	int priority;
	pthread_mutex_t* mutex;
	int log_date;
};


zlog_t* zlog_new(char* log_dir){
	zlog_t* self = (zlog_t*)malloc(sizeof(*self));
	assert(self);
	memset(self, 0, sizeof(*self));

	self->priority = LOG_INFO;
	if(log_dir == NULL){
		self->log_file = stdout;
	} else {
		strcpy(self->log_dir, log_dir);
		check_and_mk_log_dir(self->log_dir);
	}
	self->mutex = (pthread_mutex_t*)malloc(sizeof(pthread_mutex_t));
	pthread_mutex_init(self->mutex, 0); 
	return self;
}

void zlog_destroy(zlog_t** self_p){
	zlog_t* self = *self_p;
	if(self){
		if(self->log_file != stdout){
			fclose(self->log_file);
		}
		if(self->mutex){
			pthread_mutex_destroy(self->mutex);
		}
	}
}

void
zlog_set_level(zlog_t* zlog, const int priority){
	zlog->priority = priority;
}
int zlog_get_level(zlog_t* zlog){
	return zlog->priority;
}

 
FILE*
zlog_get_file(zlog_t* zlog){
	int date; 
	char fdate [32];
	if(zlog->log_file == stdout){
		return zlog->log_file;
	}

	time_t curtime = time (NULL); 
	struct tm *loctime;
	loctime = localtime (&curtime);
	strftime (fdate, 32, "%Y%m%d", loctime);
	date = atoi(fdate);

	if(date > zlog->log_date){
		char newfile[256];
		zlog->log_date = date; 
		sprintf(newfile, "%s/%s.log", zlog->log_dir, fdate);
		if(zlog->log_file){
			fclose(zlog->log_file);
		}
		zlog->log_file = fopen(newfile, "a+");
		if(!zlog->log_file){
			printf("create log file error[%s],using stdout instead\n", newfile);
			zlog->log_file = stdout;
		}
	}
	return zlog->log_file;
}



void zlog_head(zlog_t* zlog, const int priority){
	FILE* file;
	time_t curtime = time (NULL);
	struct tm *loctime;
	char formatted [32];
	char *caption;

	loctime = localtime (&curtime);
	file = zlog_get_file(zlog);
	strftime (formatted, 32, "[%Y-%m-%d %H:%M:%S", loctime);
	fprintf (file, "%s.%03d] ", formatted, current_time()%1000); 



	switch(priority)
	{
	case LOG_DEBUG: 
		caption = "DEBUG";
		break;
	case LOG_INFO: 
		caption = "INFO";
		break;
	case LOG_NOTICE: 
		caption = "NOTICE";
		break;
	case LOG_WARNING: 
		caption = "WARNING";
		break;
	case LOG_ERR: 
		caption = "ERROR";
		break;
	case LOG_CRIT: 
		caption = "CRIT";
		break;
	case LOG_ALERT: 
		caption = "ALERT";
		break;
	case LOG_EMERG: 
		caption = "EMERG";
		break;
	default: 
		caption = "UNKOWN";
		break;
	}

	fprintf (file, "%s - ", caption);
}

void zlog_raw(zlog_t* zlog, const char *format, ...){
	FILE* file = zlog_get_file(zlog);
	va_list argptr;
	va_start (argptr, format);
	vfprintf ((file), format, argptr);
	va_end (argptr);
	fflush (file);
}
static int zlog_priority(zlog_t* zlog){
	return zlog->priority;
}
static pthread_mutex_t* zlog_mutex(zlog_t* zlog){
	return zlog->mutex;
}
#define _DO_LOG(zlog, priority)\
	if(zlog_priority(zlog)< (priority)){\
		return;\
	}\
	pthread_mutex_lock(zlog_mutex(zlog));\
	zlog_head(zlog, (priority));\
	{\
		FILE* file = zlog_get_file(zlog);\
		va_list argptr;\
		va_start (argptr, format);\
		vfprintf ((file), format, argptr);\
		va_end (argptr);\
		fprintf (file, "\n");\
		fflush (file);\
	}\
	pthread_mutex_unlock(zlog_mutex(zlog));

void
zlog(zlog_t* zlog, const char *format, ...){  
	_DO_LOG(zlog, LOG_INFO);
} 


void
zlog_ex(zlog_t* zlog, const int priority, const char *format, ...){  
	_DO_LOG(zlog, priority);
} 

void zlog_emerg(zlog_t* zlog, const char *format, ...){
	_DO_LOG(zlog, LOG_EMERG);
}
void zlog_crit(zlog_t* zlog, const char *format, ...){
	_DO_LOG(zlog, LOG_CRIT);
}
void zlog_alert(zlog_t* zlog, const char *format, ...){
	_DO_LOG(zlog, LOG_ALERT);
}
void zlog_error(zlog_t* zlog, const char *format, ...){
	_DO_LOG(zlog, LOG_ERR);
}
void zlog_warning(zlog_t* zlog, const char *format, ...){
	_DO_LOG(zlog, LOG_WARNING);
}
void zlog_notice(zlog_t* zlog, const char *format, ...){
	_DO_LOG(zlog, LOG_NOTICE);
}
void zlog_info(zlog_t* zlog, const char *format, ...){
	_DO_LOG(zlog, LOG_INFO);
}
void zlog_debug(zlog_t* zlog, const char *format, ...){
	_DO_LOG(zlog, LOG_DEBUG);
}




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
		if (mkdir(base_path) != 0){
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