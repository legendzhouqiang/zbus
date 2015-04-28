#/usr/bin
JAVA_HOME=/apps/jdk7
ZBUS_HOME=..
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms64m -Xmx512m -XX:+UseParallelGC"
MAIN_CLASS=org.zstacks.zbus.server.TrackServer
MAIN_OPTS="-p 16666"
LIB_OPTS="$ZBUS_HOME/lib:$ZBUS_HOME/lib/*:$ZBUS_HOME%/conf"
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp $LIB_OPTS $MAIN_CLASS $MAIN_OPTS &
