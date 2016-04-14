REM SET JAVA_HOME=D:\SDK\jdk6_x64
SET ZBUS_HOME=..
SET JAVA_OPTS=-server -Xms64m -Xmx1024m -XX:+UseParallelGC
SET MAIN_CLASS=org.zbus.broker.ha.TrackServer
SET MAIN_OPTS=-h 0.0.0.0 -p 16667 -verbose true
SET LIB_OPTS=%ZBUS_HOME%/enhancement/*;%ZBUS_HOME%/lib/*;%ZBUS_HOME%/classes;%ZBUS_HOME%/*;
IF NOT EXIST "%JAVA_HOME%" (
    SET JAVA=java
) ELSE (
    SET JAVA=%JAVA_HOME%\bin\java
)
"%JAVA%" %JAVA_OPTS% -cp %LIB_OPTS% %MAIN_CLASS% %MAIN_OPTS%  