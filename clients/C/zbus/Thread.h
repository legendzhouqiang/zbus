#ifndef __ZBUS_THREADING_H__
#define __ZBUS_THREADING_H__ 

#include "Platform.h"
 

ZBUS_API int64_t current_millis(void) {
#if defined (__UNIX__)
	struct timeval tv;
	gettimeofday(&tv, NULL);

	return (int64_t)((int64_t)tv.tv_sec * 1000 + (int64_t)tv.tv_usec / 1000);
#elif (defined (__WINDOWS__))
	FILETIME ft;
	GetSystemTimeAsFileTime(&ft);
	return (int64_t)(*((int64_t *)(&ft)) / 10000);
#endif
}

ZBUS_API void
replace_sleep(int64_t msecs) {
#if defined (__UNIX__)
	struct timespec t;
	t.tv_sec = msecs / 1000;
	t.tv_nsec = (msecs % 1000) * 1000000;
	nanosleep(&t, NULL);
#elif (defined (__WINDOWS__))
	//  Windows XP/2000:  A value of zero causes the thread to relinquish the
	//  remainder of its time slice to any other thread of equal priority that is
	//  ready to run. If there are no other threads of equal priority ready to run,
	//  the function returns immediately, and the thread continues execution. This
	//  behavior changed starting with Windows Server 2003.
#   if defined (NTDDI_VERSION) && defined (NTDDI_WS03) && (NTDDI_VERSION >= NTDDI_WS03)
	Sleep(msecs);
#   else
	if (msecs > 0)
		Sleep(msecs);
#   endif
#endif
}
#define sleep replace_sleep

#if defined (__WINDOWS__)
 
/* Signals */
#define SIGNULL  0 /* Null	Check access to pid*/
#define SIGHUP	 1 /* Hangup	Terminate; can be trapped*/
#define SIGINT	 2 /* Interrupt	Terminate; can be trapped */
#define SIGQUIT	 3 /* Quit	Terminate with core dump; can be trapped */
#define SIGTRAP  5
#define SIGBUS   7
#define SIGKILL	 9 /* Kill	Forced termination; cannot be trapped */
#define SIGPIPE 13
#define SIGALRM 14
#define SIGTERM	15 /* Terminate	Terminate; can be trapped  */
#define SIGSTOP 17
#define SIGTSTP 18
#define SIGCONT 19
#define SIGCHLD 20
#define SIGTTIN 21
#define SIGTTOU 22
#define SIGABRT 22
/* #define SIGSTOP	24 /*Pause the process; cannot be trapped*/
/* #define SIGTSTP	25 /*Terminal stop	Pause the process; can be trapped*/
/* #define SIGCONT	26 */
#define SIGWINCH 28
#define SIGUSR1  30
#define SIGUSR2  31

#define ucontext_t void*

#define SA_NOCLDSTOP    0x00000001u
#define SA_NOCLDWAIT    0x00000002u
#define SA_SIGINFO      0x00000004u
#define SA_ONSTACK      0x08000000u
#define SA_RESTART      0x10000000u
#define SA_NODEFER      0x40000000u
#define SA_RESETHAND    0x80000000u
#define SA_NOMASK       SA_NODEFER
#define SA_ONESHOT      SA_RESETHAND
#define SA_RESTORER     0x04000000

#define sigemptyset(pset)    (*(pset) = 0)
#define sigfillset(pset)     (*(pset) = (unsigned int)-1)
#define sigaddset(pset, num) (*(pset) |= (1L<<(num)))
#define sigdelset(pset, num) (*(pset) &= ~(1L<<(num)))
#define sigismember(pset, num) (*(pset) & (1L<<(num)))

#ifndef SIG_SETMASK
#define SIG_SETMASK (0)
#define SIG_BLOCK   (1)
#define SIG_UNBLOCK (2)
#endif /*SIG_SETMASK*/

typedef	void (*__p_sig_fn_t)(int);
typedef int pid_t;

#ifndef _SIGSET_T_
#define _SIGSET_T_
#ifdef _WIN64
typedef unsigned long long _sigset_t;
#else
typedef unsigned long _sigset_t;
#endif
# define sigset_t _sigset_t
#endif /* _SIGSET_T_ */

struct sigaction {
	int          sa_flags;
	sigset_t     sa_mask;
	__p_sig_fn_t sa_handler;
	__p_sig_fn_t sa_sigaction;
};

ZBUS_API int sigaction(int sig, struct sigaction *in, struct sigaction *out);

#define pthread_mutex_t CRITICAL_SECTION
#define pthread_attr_t ssize_t

#define pthread_mutex_init(a,b) (InitializeCriticalSectionAndSpinCount((a), 0x80000400),0)
#define pthread_mutex_destroy(a) DeleteCriticalSection((a))
#define pthread_mutex_lock EnterCriticalSection
#define pthread_mutex_unlock LeaveCriticalSection

#define pthread_equal(t1, t2) ((t1) == (t2))

#define pthread_attr_init(x) (*(x) = 0)
#define pthread_attr_getstacksize(x, y) (*(y) = *(x))
#define pthread_attr_setstacksize(x, y) (*(x) = y)

#define pthread_t u_int

ZBUS_API int pthread_create(pthread_t *thread, const void *unused,
				   void *(*start_routine)(void*), void *arg);

ZBUS_API pthread_t pthread_self(void);

typedef struct {
	CRITICAL_SECTION waiters_lock;
	LONG waiters;
	int was_broadcast;
	HANDLE sema;
	HANDLE continue_broadcast;
} pthread_cond_t;

ZBUS_API int pthread_cond_init(pthread_cond_t *cond, const void *unused);
ZBUS_API int pthread_cond_destroy(pthread_cond_t *cond);
ZBUS_API int pthread_cond_wait(pthread_cond_t *cond, pthread_mutex_t *mutex);

ZBUS_API int pthread_cond_signal(pthread_cond_t *cond);
ZBUS_API int pthread_cond_broadcast(pthread_cond_t *cond);

ZBUS_API int  pthread_detach (pthread_t thread);
ZBUS_API int  pthread_sigmask(int how, const sigset_t *set, sigset_t *oldset);
ZBUS_API void pthread_exit(void *value_ptr);
ZBUS_API int  pthread_join(pthread_t* thread, void **retval);

//NOT compatible to pthread
ZBUS_API int pthread_cond_timedwait(pthread_cond_t* cond, pthread_mutex_t* mutex, int64_t millis);


#ifndef siginfo_t
typedef struct {
	int si_signo;
	int si_code;
	int si_value;
	int si_errno;
	pid_t si_pid;
	int si_uid;
	void *si_addr;
	int si_status;
	int si_band;
} siginfo_t;
#endif  

#endif /* WIN32 */  




#if defined (__WINDOWS__)

//BEGIN of WINDOWS mock of pthread

#define _NOTUSED(V) ((void) V)
#define _THREAD_STACK_SIZE (1024*1024*4)


/* Behaves as posix, works without ifdefs, makes compiler happy */
int sigaction(int sig, struct sigaction *in, struct sigaction *out) {
	_NOTUSED(out);

	/* When the SA_SIGINFO flag is set in sa_flags then sa_sigaction
	* is used. Otherwise, sa_handler is used */
	if (in->sa_flags & SA_SIGINFO)
		signal(sig, in->sa_sigaction);
	else
		signal(sig, in->sa_handler);

	return 0;
}

typedef struct thread_params {
	void *(*func)(void *);
	void * arg;
} thread_params;

static unsigned __stdcall win32_proxy_threadproc(void *arg) {

	thread_params *p = (thread_params *)arg;
	p->func(p->arg);

	free(p);

	_endthreadex(0);
	return 0;
}

int pthread_create(pthread_t *thread, const void *unused,
	void *(*start_routine)(void*), void *arg) {
	HANDLE h;
	thread_params *params = (thread_params *)malloc(sizeof(thread_params));
	_NOTUSED(unused);

	params->func = start_routine;
	params->arg = arg;

	h = (HANDLE)_beginthreadex(NULL,  /* Security not used */
		_THREAD_STACK_SIZE, /* Set custom stack size */
		win32_proxy_threadproc,  /* calls win32 stdcall proxy */
		params, /* real threadproc is passed as paremeter */
		STACK_SIZE_PARAM_IS_A_RESERVATION,  /* reserve stack */
		thread /* returned thread id */
	);
	if (!h)
		return errno;

	CloseHandle(h);
	return 0;
}

/* Noop in windows */
int pthread_detach(pthread_t thread) {
	_NOTUSED(thread);
	return 0; /* noop */
}

pthread_t pthread_self(void) {
	return GetCurrentThreadId();
}

int pthread_sigmask(int how, const sigset_t *set, sigset_t *oset) {
	_NOTUSED(set);
	_NOTUSED(oset);
	switch (how) {
	case SIG_BLOCK:
	case SIG_UNBLOCK:
	case SIG_SETMASK:
		break;
	default:
		errno = EINVAL;
		return -1;
	}

	errno = ENOSYS;
	return 0;
}

int pthread_join(pthread_t *thread, void **value_ptr) {
	int result;
	HANDLE h = OpenThread(SYNCHRONIZE, FALSE, *thread);
	_NOTUSED(value_ptr);

	switch (WaitForSingleObject(h, INFINITE)) {
	case WAIT_OBJECT_0:
		result = 0;
	case WAIT_ABANDONED:
		result = EINVAL;
	default:
		result = GetLastError();
	}

	CloseHandle(h);
	return result;
}

int pthread_cond_init(pthread_cond_t *cond, const void *unused) {
	_NOTUSED(unused);
	cond->waiters = 0;
	cond->was_broadcast = 0;

	InitializeCriticalSection(&cond->waiters_lock);

	cond->sema = CreateSemaphore(NULL, 0, LONG_MAX, NULL);
	if (!cond->sema) {
		errno = GetLastError();
		return -1;
	}

	cond->continue_broadcast = CreateEvent(NULL,    /* security */
		FALSE,                  /* auto-reset */
		FALSE,                  /* not signaled */
		NULL);                  /* name */
	if (!cond->continue_broadcast) {
		errno = GetLastError();
		return -1;
	}

	return 0;
}

int pthread_cond_destroy(pthread_cond_t *cond) {
	CloseHandle(cond->sema);
	CloseHandle(cond->continue_broadcast);
	DeleteCriticalSection(&cond->waiters_lock);
	return 0;
}

int pthread_cond_timedwait(pthread_cond_t* cond, pthread_mutex_t* mutex, int64_t millis) {
	int last_waiter;

	EnterCriticalSection(&cond->waiters_lock);
	cond->waiters++;
	LeaveCriticalSection(&cond->waiters_lock);

	/*
	* Unlock external mutex and wait for signal.
	* NOTE: we've held mutex locked long enough to increment
	* waiters count above, so there's no problem with
	* leaving mutex unlocked before we wait on semaphore.
	*/
	LeaveCriticalSection(mutex);

	/* let's wait - ignore return value */
	WaitForSingleObject(cond->sema, millis);

	/*
	* Decrease waiters count. If we are the last waiter, then we must
	* notify the broadcasting thread that it can continue.
	* But if we continued due to cond_signal, we do not have to do that
	* because the signaling thread knows that only one waiter continued.
	*/
	EnterCriticalSection(&cond->waiters_lock);
	cond->waiters--;
	last_waiter = cond->was_broadcast && cond->waiters == 0;
	LeaveCriticalSection(&cond->waiters_lock);

	if (last_waiter) {
		/*
		* cond_broadcast was issued while mutex was held. This means
		* that all other waiters have continued, but are contending
		* for the mutex at the end of this function because the
		* broadcasting thread did not leave cond_broadcast, yet.
		* (This is so that it can be sure that each waiter has
		* consumed exactly one slice of the semaphor.)
		* The last waiter must tell the broadcasting thread that it
		* can go on.
		*/
		SetEvent(cond->continue_broadcast);
		/*
		* Now we go on to contend with all other waiters for
		* the mutex. Auf in den Kampf!
		*/
	}
	/* lock external mutex again */
	EnterCriticalSection(mutex);

	return 0;
}

int pthread_cond_wait(pthread_cond_t *cond, pthread_mutex_t *mutex) {
	return pthread_cond_timedwait(cond, mutex, INFINITE);
}

/*
* IMPORTANT: This implementation requires that pthread_cond_signal
* is called while the mutex is held that is used in the corresponding
* pthread_cond_wait calls!
*/
int pthread_cond_signal(pthread_cond_t *cond) {
	int have_waiters;

	EnterCriticalSection(&cond->waiters_lock);
	have_waiters = cond->waiters > 0;
	LeaveCriticalSection(&cond->waiters_lock);

	/*
	* Signal only when there are waiters
	*/
	if (have_waiters)
		return ReleaseSemaphore(cond->sema, 1, NULL) ? 0 : GetLastError();
	else
		return 0;
}

void pthread_exit(void *value_ptr) {
	ExitThread((DWORD)value_ptr);
}


int pthread_cond_broadcast(pthread_cond_t *cond) {
	EnterCriticalSection(&cond->waiters_lock);
	if ((cond->was_broadcast = cond->waiters > 0)) {
		/* wake up all waiters */
		ReleaseSemaphore(cond->sema, cond->waiters, NULL);
		LeaveCriticalSection(&cond->waiters_lock);
		/*
		* At this point all waiters continue. Each one takes its
		* slice of the semaphor. Now it's our turn to wait: Since
		* the external mutex is held, no thread can leave cond_wait,
		* yet. For this reason, we can be sure that no thread gets
		* a chance to eat *more* than one slice. OTOH, it means
		* that the last waiter must send us a wake-up.
		*/
		WaitForSingleObject(cond->continue_broadcast, INFINITE);
		/*
		* Since the external mutex is held, no thread can enter
		* cond_wait, and, hence, it is safe to reset this flag
		* without cond->waiters_lock held.
		*/
		cond->was_broadcast = 0;
	}
	else {
		LeaveCriticalSection(&cond->waiters_lock);
	}

	return 0;
}

#endif 


#endif
