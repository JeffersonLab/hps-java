#!/bin/sh
et=/local/celentano/eclipse_workspace/hps/et/target/hps-et-3.0.2-SNAPSHOT.jar
classpath=${et}:`pwd`/jars/jevio-1.0.jar
prod="java -classpath $classpath org.jlab.coda.et.apps.EvioProducer -f ETBuffer -host $(hostname) -s 1024"
echo $prod
exec $prod
