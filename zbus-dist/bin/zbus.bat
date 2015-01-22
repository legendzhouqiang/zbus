REM SET JAVA_HOME=%JAVA_HOME%\bin\
SET JAVA_OPTS=-Dfile.encoding=UTF-8 -server -Xms1024m -Xmx4096m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.server.ZbusServer
SET MAIN_OPTS=-p 15555 -w 3000 -track 127.0.0.1:16666;127.0.0.1:16667 
START /B java %JAVA_OPTS% -cp "../lib/*;../lib/logging/*;../*;" %MAIN_CLASS% %MAIN_OPTS% 