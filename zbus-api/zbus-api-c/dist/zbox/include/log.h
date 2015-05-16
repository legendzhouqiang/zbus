#ifndef LOG_H
#define LOG_H

#include "platform.h" 

#ifdef __cplusplus
extern "C" {
#endif

#define	LOG_EMERG	0	/* system is unusable */
#define	LOG_ALERT	1	/* action must be taken immediately */
#define	LOG_CRIT	2	/* critical conditions */
#define	LOG_ERR		3	/* error conditions */
#define	LOG_WARNING	4	/* warning conditions */
#define	LOG_NOTICE	5	/* normal but significant condition */
#define	LOG_INFO	6	/* informational */
#define	LOG_DEBUG	7	/* debug-level messages */

typedef struct _zlog zlog_t;

ZBOX_EXPORT	zlog_t* 
	zlog_new(char* log_dir);
ZBOX_EXPORT void 
	zlog_destroy(zlog_t** self_p);
ZBOX_EXPORT void
	zlog_set_level(zlog_t* zlog, const int priority);
ZBOX_EXPORT int
	zlog_get_level(zlog_t* zlog);
ZBOX_EXPORT FILE*
	zlog_get_file(zlog_t* zlog);

ZBOX_EXPORT void
	zlog(zlog_t* zlog, const char *format, ...);

ZBOX_EXPORT void
	zlog_ex(zlog_t* zlog, const int priority, const char *format, ...);

ZBOX_EXPORT void zlog_raw(zlog_t* zlog, const char *format, ...);

ZBOX_EXPORT void zlog_head(zlog_t* zlog, const int priority);
ZBOX_EXPORT void zlog_emerg(zlog_t* zlog, const char *format, ...);
ZBOX_EXPORT void zlog_crit(zlog_t* zlog, const char *format, ...);
ZBOX_EXPORT void zlog_alert(zlog_t* zlog, const char *format, ...);
ZBOX_EXPORT void zlog_error(zlog_t* zlog, const char *format, ...);
ZBOX_EXPORT void zlog_warning(zlog_t* zlog, const char *format, ...);
ZBOX_EXPORT void zlog_notice(zlog_t* zlog, const char *format, ...);
ZBOX_EXPORT void zlog_info(zlog_t* zlog, const char *format, ...);
ZBOX_EXPORT void zlog_debug(zlog_t* zlog, const char *format, ...);

#ifdef __cplusplus
}
#endif

#endif

