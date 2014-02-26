#!/bin/sh

et=/local/celentano/eclipse_workspace/hps/et/target/hps-et-3.0.2-SNAPSHOT.jar
server="java -Xmx1024m -classpath $et org.jlab.coda.et.apps.StartEt $@" 

echo "$server"
exec $server
