#include "threading.h"


int64_t current_millis(void) {
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

    thread_params *p = (thread_params *) arg;
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
    params->arg  = arg;

    h =(HANDLE) _beginthreadex(NULL,  /* Security not used */
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
int pthread_detach (pthread_t thread) {
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

int pthread_join(pthread_t *thread, void **value_ptr)  {
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

int pthread_cond_wait(pthread_cond_t *cond, pthread_mutex_t *mutex) {
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
    WaitForSingleObject(cond->sema, INFINITE);

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

void pthread_exit(void *value_ptr){
	ExitThread((DWORD)value_ptr);
}


int pthread_cond_broadcast(pthread_cond_t *cond){
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
	} else {
		LeaveCriticalSection(&cond->waiters_lock);
	}

	return 0;
}

#endif

