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

ZBOX_EXPORT void
	zlog_set_stdout();

ZBOX_EXPORT void
	zlog_set_level(const int priority);

ZBOX_EXPORT int
	zlog_get_level();

ZBOX_EXPORT void
	zlog_set_file(char* base_path);

ZBOX_EXPORT FILE*
	zlog_get_file();

ZBOX_EXPORT void
	zlog(const char *format, ...);

ZBOX_EXPORT void
	zlog_ex(const int priority, const char *format, ...);

ZBOX_EXPORT void zlog_head(const int priority);
ZBOX_EXPORT void zlog_emerg(const char *format, ...);
ZBOX_EXPORT void zlog_crit(const char *format, ...);
ZBOX_EXPORT void zlog_alert(const char *format, ...);
ZBOX_EXPORT void zlog_error(const char *format, ...);
ZBOX_EXPORT void zlog_warning(const char *format, ...);
ZBOX_EXPORT void zlog_notice(const char *format, ...);
ZBOX_EXPORT void zlog_info(const char *format, ...);
ZBOX_EXPORT void zlog_debug(const char *format, ...);

#ifdef __cplusplus
}
#endif

#endif

