#!/bin/sh

et=/local/celentano/eclipse_workspace/hps/et/target/hps-et-3.0.2-SNAPSHOT.jar
prod="java -classpath $et org.jlab.coda.et.apps.Blaster -f ETBuffer -host localhost"
echo $prod
exec $prod
