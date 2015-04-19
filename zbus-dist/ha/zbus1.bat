SET JAVA_HOME=D:\SDK\jdk6_x64
SET ZBUS_HOME=..
SET JAVA_OPTS=-Dfile.encoding=UTF-8 -server -Xms1024m -Xmx4096m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.server.ZbusServer
REM -p 15555 -store dummy|sql|redis  (hsqldb preferred)
SET MAIN_OPTS=-p 15555 -store dummy -openBrowser true
SET LIB_OPTS=%ZBUS_HOME%/lib;%ZBUS_HOME%/lib/*;%ZBUS_HOME%/conf;
START /B %JAVA_HOME%\bin\java %JAVA_OPTS% -cp %LIB_OPTS% %MAIN_CLASS% %MAIN_OPTS% 