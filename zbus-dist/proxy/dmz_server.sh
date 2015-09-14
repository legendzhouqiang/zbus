#/usr/bin
JAVA_HOME=/apps/jdk7
ZBUS_HOME=..
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms64m -Xmx1024m -XX:+UseParallelGC"
MAIN_CLASS=org.zbus.proxy.DmzServer
MAIN_OPTS="-up 8080 -down 15557 -notify 15558"
LIB_OPTS="$ZBUS_HOME/lib:$ZBUS_HOME/*"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &
