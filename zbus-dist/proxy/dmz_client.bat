REM SET JAVA_HOME=D:\SDK\jdk6_x64

SET ZBUS_HOME=..
SET JAVA_OPTS=-server -Xms64m -Xmx1024m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.proxy.DmzClient
SET MAIN_OPTS=-dmzDown 127.0.0.1:15557 -dmzNotify 127.0.0.1:15558 -target 127.0.0.1:15555
SET LIB_OPTS=%ZBUS_HOME%/lib;%ZBUS_HOME%/*;

IF NOT EXIST "%JAVA_HOME%" (
    SET JAVA=java 
) ELSE (
    SET JAVA=%JAVA_HOME%\bin\java
)
"%JAVA%" %JAVA_OPTS% -cp %LIB_OPTS% %MAIN_CLASS% %MAIN_OPTS% 