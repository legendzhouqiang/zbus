#include "include/prelude.h"

#if (defined (__WINDOWS__)) 

#ifndef __RTL_GENRANDOM
#define __RTL_GENRANDOM 1
typedef BOOLEAN (_stdcall* RtlGenRandomFunc)(void * RandomBuffer, ULONG RandomBufferLength);
#endif
RtlGenRandomFunc RtlGenRandom;

int random() {
#if defined(_WIN64) || defined(_MSC_VER)
    unsigned int x=0;
    RtlGenRandom(&x, sizeof(UINT_MAX));
    return (int)(x >> 1);
#else
    unsigned int x=0;
    RtlGenRandom(&x, sizeof(UINT_MAX));
    return (int)(x >> 1);
#endif
}

#endif
 


char*
option(int argc, char* argv[], char* opt, char* default_value){
	int i,len;
	char* value = default_value;
	for(i=1; i<argc; i++){
		len = strlen(opt);
		if(len> strlen(argv[i])) len = strlen(argv[i]);
		if(strncmp(argv[i],opt,len)==0){
			value = &argv[i][len];
		}
	}
	return value;
}


///////////////////////////////MEMORY ALLOCATION//////////////////////////
#define PREFIX_SIZE (sizeof(size_t))
static size_t used_memory = 0; //thread unsafe
void*
zmalloc(size_t size) {
	void *ptr = malloc(size+PREFIX_SIZE);
	if (!ptr){
		fprintf(stderr, "zmalloc: Out of memory trying to allocate %lu bytes\n",size);
		fflush(stderr);
		abort();
	}
	*((size_t*)ptr) = size;
	used_memory += (size+PREFIX_SIZE); 
	return (char*)ptr+PREFIX_SIZE;
}

void *zcalloc(size_t size) {
	void *ptr = calloc(1, size+PREFIX_SIZE);
	if (!ptr){
		fprintf(stderr, "zcalloc: Out of memory trying to allocate %lu bytes\n",size);
		fflush(stderr);
		abort();
	}
	*((size_t*)ptr) = size;
	used_memory += (size+PREFIX_SIZE); 
	return (char*)ptr+PREFIX_SIZE; 
}


void*
zrealloc(void *ptr, size_t size) { 
	if (ptr == NULL) return zmalloc(size);

	void* realptr = (char*)ptr-PREFIX_SIZE;
	size_t oldsize = *((size_t*)realptr);
	void* newptr = realloc(realptr,size+PREFIX_SIZE);
	if (!newptr){
		fprintf(stderr, "zrealloc: Out of memory trying to allocate %lu bytes\n",size);
		fflush(stderr);
		abort();
	}
	*((size_t*)newptr) = size;
	used_memory -= oldsize; 
	used_memory += size;  

	return (char*)newptr+PREFIX_SIZE; 
}

void 
zfree(void *ptr) {
	if(!ptr) return;

	void* realptr = (char*)ptr-PREFIX_SIZE;
	size_t oldsize = *((size_t*)realptr);
	used_memory -= (oldsize+PREFIX_SIZE);
	free(realptr);
}

char*
zstrdup(const char *s){
	if(s == NULL) return NULL;
	size_t l = strlen(s)+1;
	char *p = (char*)zmalloc(l);
	memcpy(p,s,l);
	return p;
} 

size_t 
zmalloc_used_memory(void) {
	return used_memory; 
}

void
zmalloc_report(){
	printf("[MEM]: %ld \n", used_memory);
}


///////////////////////////////////////mutex////////////////////////////////
struct _zmutex_t {
#if defined (__UNIX__)
	pthread_mutex_t mutex;
#elif defined (__WINDOWS__)
	CRITICAL_SECTION mutex;
#endif
};


zmutex_t *
zmutex_new (void){
	zmutex_t *self;
	self = (zmutex_t *) zmalloc (sizeof (zmutex_t));
#if defined (__UNIX__)
	pthread_mutex_init (&self->mutex, NULL);
#elif defined (__WINDOWS__)
	InitializeCriticalSection (&self->mutex);
#endif
	return self;
}

void
zmutex_destroy (zmutex_t **self_p){
	if (!self_p) return;
	zmutex_t *self = *self_p;
	if (self) { 
#if defined (__UNIX__)
		pthread_mutex_destroy (&self->mutex);
#elif defined (__WINDOWS__)
		DeleteCriticalSection (&self->mutex);
#endif
		zfree (self);
		*self_p = NULL;
	}
}


void
zmutex_lock (zmutex_t *self){
#if defined (__UNIX__)
	pthread_mutex_lock (&self->mutex);
#elif defined (__WINDOWS__)
	EnterCriticalSection (&self->mutex);
#endif
}


void
zmutex_unlock (zmutex_t *self){
#if defined (__UNIX__)
	pthread_mutex_unlock (&self->mutex);
#elif defined (__WINDOWS__)
	LeaveCriticalSection (&self->mutex);
#endif
}

/////////////////////////////////////thread////////////////////////////////////

typedef struct{
	thread_fn func;
	void * args;
} thread_params_t;

#if defined (__UNIX__)
// Thread shim for UNIX calls the real thread and cleans up afterwards.
void *
s_thread_params (void *args){
	assert (args);
	thread_params_t *params = (thread_params_t *) args;
	if(params->func)
		params->func(params->args); 
	zfree (params);
	return NULL;
}

#elif defined (__WINDOWS__)
// Thread shim for Windows that wraps a POSIX-style thread handler
// and does the _endthreadex for us automatically.
unsigned __stdcall
s_thread_params (void *args){
	assert (args);
	thread_params_t *params = (thread_params_t *) args;
	if(params->func)
		params->func(params->args); 
	zfree (params);
	_endthreadex (0); // Terminates thread
	return 0;
}
#endif
 

zthread_t
zthread_new (thread_fn func, void *args){
	// Prepare argument shim for child thread
	thread_params_t *params = (thread_params_t *) zmalloc (sizeof (thread_params_t));
	assert(params);
	params->func = func;
	params->args = args;
	zthread_t thread;

#if defined (__UNIX__)
	pthread_create (&thread, NULL, s_thread_params, params);
	//pthread_detach (thread);

#elif defined (__WINDOWS__)
	thread = (zthread_t)_beginthreadex(
		NULL, // Handle is private to this process
		0, // Use a default stack size for new thread
		s_thread_params, // Start real thread function via this shim
		params, // Which gets the current object as argument
		CREATE_SUSPENDED, // Set thread priority before starting it
		NULL); // We don't use the thread ID

	assert (thread);
	// Set child thread priority to same as current
	int priority = GetThreadPriority (GetCurrentThread ());
	SetThreadPriority (thread, priority);
	// Now start thread
	ResumeThread (thread);
#endif

	return thread;
}

void
zthread_join(zthread_t thread){
	
#if defined(__WINDOWS__)
	WaitForSingleObject(thread, INFINITE);
	CloseHandle(thread);
#elif defined(__UNIX__)
	pthread_join(thread, NULL); 
#endif

}




//////////////////////////////////////clock////////////////////////////////////
#if defined (__WINDOWS__) 
static int64_t
s_filetime_to_msec (const FILETIME *ft){
	return (int64_t) (*((int64_t *) ft) / 10000);
}
#endif


//  --------------------------------------------------------------------------
//  Sleep for a number of milliseconds

void
zclock_sleep (int msecs){
#if defined (__UNIX__)
	struct timespec t;
	t.tv_sec  =  msecs / 1000;
	t.tv_nsec = (msecs % 1000) * 1000000;
	nanosleep (&t, NULL);
#elif (defined (__WINDOWS__))
	//  Windows XP/2000:  A value of zero causes the thread to relinquish the
	//  remainder of its time slice to any other thread of equal priority that is
	//  ready to run. If there are no other threads of equal priority ready to run,
	//  the function returns immediately, and the thread continues execution. This
	//  behavior changed starting with Windows Server 2003.
#   if defined (NTDDI_VERSION) && defined (NTDDI_WS03) && (NTDDI_VERSION >= NTDDI_WS03)
	Sleep (msecs);
#   else
	if (msecs > 0)
		Sleep (msecs);
#   endif
#endif
}

 
int64_t
zclock_time (void){
#if defined (__UNIX__)
	struct timeval tv;
	gettimeofday (&tv, NULL);
	return (int64_t) ((int64_t) tv.tv_sec * 1000 + (int64_t) tv.tv_usec / 1000);
#elif (defined (__WINDOWS__))
	FILETIME ft;
	GetSystemTimeAsFileTime (&ft);
	return s_filetime_to_msec (&ft);
#endif
}


///////////////////////////////////////log//////////////////////////////////
char  g_log_path[256] = {'.'};
int   g_log_date = 0;
FILE* g_log_file = NULL;
int   g_log_stdout = 1;


void
zlog_use_stdout(){ 
	g_log_stdout = 1;
}
void
zlog_use_file(char* base_path){ 
	g_log_stdout = 0;
	strcpy(g_log_path, base_path);
}

FILE*
zlog_get_log_file(){
	if(g_log_stdout) return stdout;

	time_t curtime = time (NULL);
	struct tm *loctime = localtime (&curtime);
	char fdate [32];
	strftime (fdate, 32, "%Y%m%d", loctime);
	int date = atoi(fdate);

	if(date > g_log_date){
		g_log_date = date;
		char newfile[256];
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

	return g_log_file? g_log_file : stdout;
}

void
zlog(const char *format, ...){  
	FILE* file = zlog_get_log_file();

	time_t curtime = time (NULL);
	struct tm *loctime = localtime (&curtime);
	char formatted [32];
	strftime (formatted, 32, "%Y-%m-%d %H:%M:%S", loctime);
	fprintf (file, "%s.%03d ", formatted, zclock_time()%1000); 

	va_list argptr;
	va_start (argptr, format);
	vfprintf (file, format, argptr);
	va_end (argptr); 
	fflush (file);
} 

