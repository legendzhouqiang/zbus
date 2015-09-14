#/usr/bin
JAVA_HOME=/apps/jdk7
ZBUS_HOME=..
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms64m -Xmx1024m -XX:+UseParallelGC"
MAIN_CLASS=org.zbus.proxy.DmzClient
MAIN_OPTS="-dmzDown 127.0.0.1:15557 -dmzNotify 127.0.0.1:15558 -target 127.0.0.1:15555"
LIB_OPTS="$ZBUS_HOME/lib:$ZBUS_HOME/*"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &
