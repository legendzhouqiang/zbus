#include "logger.h" 

char  g_log_path[256] = {'.'};
int   g_log_date = 0;
FILE* g_log_file = NULL; 
int   g_use_stdout = 1;
int   g_priority = LOG_INFO;

static int file_exists(const char* path){

#if defined (__UNIX__)
	return access(path, 0) == 0;
#elif (defined (__WINDOWS__))
	DWORD dwAttrib = GetFileAttributes(path);
	return (dwAttrib != INVALID_FILE_ATTRIBUTES);
#endif

}
static int check_and_mk_log_dir(const char *base_path)
{
	if (!file_exists(base_path))
	{
		if (mkdir(base_path, 0755) != 0)
		{
			fprintf(stderr, "mkdir \"%s\" fail, " \
				"errno: %d, error info: %s", \
				base_path, errno, strerror(errno));
			return errno != 0 ? errno : EPERM;
		}
	}
	return 0;
}
 
int64_t current_time(void) {
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


void
zlog_set_level(const int priority){
	g_priority = priority;
}
int zlog_get_level(){
	return g_priority;
}


void
zlog_set_stdout(){ 
	g_use_stdout = 1;
	g_log_file = stdout;
}

void
zlog_set_file(char* base_path){  
	strcpy(g_log_path, base_path);
	g_use_stdout = 0;
	if( check_and_mk_log_dir(base_path) ){
		g_use_stdout = 1;
	}
}


FILE*
zlog_get_file(){
	int date; 
	char fdate [32];
	
	if(g_use_stdout){
		return stdout;
	}

	{
		time_t curtime = time (NULL); 
		struct tm *loctime;
		loctime = localtime (&curtime);
		strftime (fdate, 32, "%Y%m%d", loctime);
		date = atoi(fdate);
	}
	

	if(date > g_log_date){
		char newfile[256];
		g_log_date = date; 
		sprintf(newfile, "%s/%s.log", g_log_path, fdate);
		if(g_log_file && g_log_file != stdout){
			fclose(g_log_file);
		}
		g_log_file = fopen(newfile, "a+");
		if(!g_log_file){
			printf("create log file error[%s],using stdout instead\n", newfile);
			g_log_file = stdout;
		}
	}

	return g_log_file;
}


void zlog_head(const int priority){
	FILE* file;
	time_t curtime = time (NULL);
	struct tm *loctime;
	char formatted [32];
	char *caption;

	loctime = localtime (&curtime);
	file = zlog_get_file();
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


#define _DO_LOG(priority) \
	if(g_priority < (priority)){\
		return;\
	}\
	zlog_head((priority));\
	{\
		FILE* file = zlog_get_file();\
		va_list argptr;\
		va_start (argptr, format);\
		vfprintf ((file), format, argptr);\
		va_end (argptr);\
		fprintf (file, "\n");\
		fflush (file);\
	}
	


void
zlog(const char *format, ...){  
	_DO_LOG(LOG_INFO);
} 


void
zlog_ex(const int priority, const char *format, ...){  
	_DO_LOG(priority);
} 

void zlog_emerg(const char *format, ...){
	_DO_LOG(LOG_EMERG);
}
void zlog_crit(const char *format, ...){
	_DO_LOG(LOG_CRIT);
}
void zlog_alert(const char *format, ...){
	_DO_LOG(LOG_ALERT);
}
void zlog_error(const char *format, ...){
	_DO_LOG(LOG_ERR);
}
void zlog_warning(const char *format, ...){
	_DO_LOG(LOG_WARNING);
}
void zlog_notice(const char *format, ...){
	_DO_LOG(LOG_NOTICE);
}
void zlog_info(const char *format, ...){
	_DO_LOG(LOG_INFO);
}
void zlog_debug(const char *format, ...){
	_DO_LOG(LOG_DEBUG);
}
