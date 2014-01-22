#!/bin/sh

#
# Run the ET GUI Monitor.
#

et=${project.basedir}/target/${project.artifactId}-${project.version}.jar
classpath=${et}:${project.basedir}/jars/jlayout30.jar:${project.basedir}/jars/jlm20.jar:${project.basedir}/jars/jloox20.jar
monitor="java -classpath $classpath org.jlab.coda.et.monitorGui.Monitor"
echo "$monitor"
exec $monitor
