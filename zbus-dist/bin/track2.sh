#/usr/bin
JAVA_HOME=/apps/jdk7
JAVA_OPTS="-Dfile.encoding=UTF-8 -server -Xms1024m -Xmx4096m -XX:+UseParallelGC"
MAIN_CLASS=org.zbus.server.TrackServer
MAIN_OPTS="-p 16667" 
nohup $JAVA_HOME/bin/java $JAVA_OPTS -cp "../lib/*:../lib/logging/*:../zbus-5.0.0.jar" $MAIN_CLASS $MAIN_OPTS &
