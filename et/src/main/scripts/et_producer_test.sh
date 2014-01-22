#!/bin/sh

et=${project.basedir}/target/${project.artifactId}-${project.version}.jar
prod="java -classpath $et org.jlab.coda.et.apps.Blaster -f ETBuffer -host localhost"
echo $prod
exec $prod
