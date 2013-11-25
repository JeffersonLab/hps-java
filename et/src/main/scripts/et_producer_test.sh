#!/bin/sh

et=${project.basedir}/target/hps-et-java-${project.version}.jar
prod="java -classpath $et org.jlab.coda.et.apps.Blaster -f ETBuffer -host localhost"
echo $prod
exec $prod
