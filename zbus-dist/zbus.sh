#/usr/bin
JAVA_HOME=/apps/jdk7
ZBUS_HOME=.
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms1024m -Xmx4096m -XX:+UseParallelGC"
MAIN_CLASS=org.zbus.server.ZbusServer
MAIN_OPTS="-p 15555 -store dummy -track 127.0.0.1:16666;127.0.0.1:16667"
LIB_OPTS="$ZBUS_HOME/lib:$ZBUS_HOME/lib/*:$ZBUS_HOME%/conf"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &
