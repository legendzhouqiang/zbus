#/usr/bin
JAVA_HOME=/apps/jdk7
ZBUS_HOME=.
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms64m -Xmx1024m -XX:+UseParallelGC"
MAIN_CLASS=org.zbus.mq.server.MqServer
MAIN_OPTS="-h 0.0.0.0 -p 15555 -verbose false -conf zbus.properties -store store -track"
LIB_OPTS="$ZBUS_HOME/lib:$ZBUS_HOME/*:$ZBUS_HOME/classes"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &


