#ifndef __ZBOX_THREADING_H__
#define __ZBOX_THREADING_H__ 

#include "platform.h"

#ifdef __cplusplus
extern "C" {
#endif

ZBOX_EXPORT int64_t current_millis(void);

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

int sigaction(int sig, struct sigaction *in, struct sigaction *out);



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

int pthread_create(pthread_t *thread, const void *unused,
				   void *(*start_routine)(void*), void *arg);

pthread_t pthread_self(void);

typedef struct {
	CRITICAL_SECTION waiters_lock;
	LONG waiters;
	int was_broadcast;
	HANDLE sema;
	HANDLE continue_broadcast;
} pthread_cond_t;

int pthread_cond_init(pthread_cond_t *cond, const void *unused);
int pthread_cond_destroy(pthread_cond_t *cond);
int pthread_cond_wait(pthread_cond_t *cond, pthread_mutex_t *mutex);
int pthread_cond_signal(pthread_cond_t *cond); 
int pthread_cond_broadcast(pthread_cond_t *cond);

int pthread_detach (pthread_t thread);
int pthread_sigmask(int how, const sigset_t *set, sigset_t *oldset);
void pthread_exit(void *value_ptr); 
int  pthread_join(pthread_t* thread, void **retval);


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


#ifdef __cplusplus
}
#endif


#endif
