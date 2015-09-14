REM SET JAVA_HOME=D:\SDK\jdk6_x64

SET ZBUS_HOME=..
SET JAVA_OPTS=-server -Xms64m -Xmx1024m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.proxy.DmzServer
SET MAIN_OPTS=-up 80 -down 15557 -notify 15558
SET LIB_OPTS=%ZBUS_HOME%/lib;%ZBUS_HOME%/*;

IF NOT EXIST "%JAVA_HOME%" (
    SET JAVA=java 
) ELSE (
    SET JAVA=%JAVA_HOME%\bin\java
)
%JAVA% %JAVA_OPTS% -cp %LIB_OPTS% %MAIN_CLASS% %MAIN_OPTS% 