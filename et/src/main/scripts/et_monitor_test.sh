#!/bin/sh

et=${project.basedir}/target/hps-et-java-${project.version}.jar
monitor="java -classpath $et org.jlab.coda.et.apps.EtMonitor -f ETBuffer -h localhost" 
echo "$monitor"
exec $monitor
