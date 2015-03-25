#!/bin/sh

# The bin jar to run.
jarfile=${project.build.directory}/${project.artifactId}-${project.version}-bin.jar

# Start the monitoring application with supplied arguments.
prod="java -Xmx1024m -jar $jarfile $@"
echo $prod
exec $prod
