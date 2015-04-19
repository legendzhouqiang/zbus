REM SET JAVA_HOME=D:\SDK\jdk6_x64
SET ZBUS_HOME=..
SET JAVA_OPTS=-Dfile.encoding=UTF-8 -server -Xms64m -Xmx512m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.server.TrackServer 
SET MAIN_OPTS=-p 16666
SET LIB_OPTS=%ZBUS_HOME%/lib;%ZBUS_HOME%/lib/*;%ZBUS_HOME%/conf;
START /B %JAVA_HOME%\bin\java %JAVA_OPTS% -cp %LIB_OPTS% %MAIN_CLASS% %MAIN_OPTS% 