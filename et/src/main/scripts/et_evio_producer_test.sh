#!/bin/sh
et=${project.basedir}/target/hps-et-java-${project.version}.jar
classpath=${et}:`pwd`/jars/jevio-1.0.jar
prod="java -classpath $classpath org.jlab.coda.et.apps.EvioProducer -f ETBuffer -host $(hostname) -s 1024"
echo $prod
exec $prod
