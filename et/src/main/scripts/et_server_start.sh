#!/bin/sh

et=${project.basedir}/target/hps-et-java-${project.version}.jar
server="java -Xmx1024m -classpath $et org.jlab.coda.et.apps.StartEt $@" 

echo "$server"
exec $server
