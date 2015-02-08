REM SET JAVA_HOME=%JAVA_HOME%\bin\
SET JAVA_OPTS=-Dfile.encoding=UTF-8 -server -Xms1024m -Xmx4096m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.server.ZbusServer
SET MAIN_OPTS=-p 15555 -track 127.0.0.1:16666;127.0.0.1:16667 
SET PLUGIN_DIR=./plugins
SET PLUGIN_OPTS=%PLUGIN_DIR%;%PLUGIN_DIR%/lib/*;%PLUGIN_DIR%/classes;
START /B java %JAVA_OPTS% -cp "./lib/*;./*;%PLUGIN_OPTS%" %MAIN_CLASS% %MAIN_OPTS% 