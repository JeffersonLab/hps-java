#!/bin/sh

et=${project.basedir}/target/${project.artifactId}-${project.version}.jar
monitor="java -classpath $et org.jlab.coda.et.apps.EtMonitor -f ETBuffer -h localhost" 
echo "$monitor"
exec $monitor
