#/usr/bin
JAVA_HOME=/apps/jdk7
ZBUS_HOME=.
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms64m -Xmx1024m -XX:+UseParallelGC"
MAIN_CLASS=org.zstacks.zbus.server.ZbusServer
MAIN_OPTS="-p 15555 -verbose true"
LIB_OPTS="$ZBUS_HOME/lib:$ZBUS_HOME/lib/*"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &
