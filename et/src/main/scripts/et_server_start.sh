#!/bin/sh

et=${project.basedir}/target/${project.artifactId}-${project.version}.jar
server="java -Xmx1024m -classpath $et org.jlab.coda.et.apps.StartEt $@" 

echo "$server"
exec $server
