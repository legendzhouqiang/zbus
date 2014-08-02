REM SET JAVA_HOME=%JAVA_HOME%\bin\
SET JAVA_OPTS=-Dfile.encoding=UTF-8 -server -Xms1024m -Xmx4096m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.server.TrackServer
SET MAIN_OPTS=-p 16667
START /B java %JAVA_OPTS% -cp "../lib/*;../lib/logging/*;../zbus-5.0.0.jar;" %MAIN_CLASS% %MAIN_OPTS% 