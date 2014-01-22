#!/bin/sh

# Setup server for test EVIO events. 
et=${project.basedir}/target/${project.artifactId}-${project.version}.jar
#server="java -classpath $et org.jlab.coda.et.apps.StartEt -p 11111 -f ETBuffer -n 100 -s 1024 -v -d" 
server="java -Xmx1024m -classpath $et org.jlab.coda.et.apps.StartEt -f ETBuffer" 
server=$server" -s 10000 -v"

echo "$server"
exec $server
