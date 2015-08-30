REM SET JAVA_HOME=D:\SDK\jdk6_x64
SET ZBUS_HOME=..
SET JAVA_OPTS=-server -Xms64m -Xmx1024m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.mq.server.MqServer 
SET MAIN_OPTS=-h 0.0.0.0 -p 15555 -verbose false -conf zbus.properties -store ../store -track 127.0.0.1:16666
SET LIB_OPTS=%ZBUS_HOME%/lib;%ZBUS_HOME%/*;%ZBUS_HOME%/ha/*;
"%JAVA_HOME%\bin\java" %JAVA_OPTS% -cp %LIB_OPTS% %MAIN_CLASS% %MAIN_OPTS% 