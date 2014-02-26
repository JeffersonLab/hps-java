#!/bin/sh

et=/local/celentano/eclipse_workspace/hps/et/target/hps-et-3.0.2-SNAPSHOT.jar
monitor="java -classpath $et org.jlab.coda.et.apps.EtMonitor -f ETBuffer -h localhost" 
echo "$monitor"
exec $monitor
