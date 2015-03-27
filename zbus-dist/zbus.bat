REM SET JAVA_HOME=%JAVA_HOME%\bin\
SET JAVA_OPTS=-Dfile.encoding=UTF-8 -server -Xms1024m -Xmx4096m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.server.ZbusServer
REM -p 15555 -store dummy|sql|redis  (hsqldb preferred)
SET MAIN_OPTS=-p 15555 -store sql
START /B java %JAVA_OPTS% -cp "./ext;./ext/*;./lib;./lib/*;" %MAIN_CLASS% %MAIN_OPTS% 