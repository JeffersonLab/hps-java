#!/bin/sh

# Set the load library path.
. ${project.build.directory}/scripts/ldpath.sh

# Setup the classpath variable.
classpath=${project.build.directory}/${project.artifactId}-${project.version}-bin.jar

# Start the monitoring application, sending any script arguments to the end of the command.
prod="java -Xmx1024m -classpath $classpath org.hps.monitoring.MonitoringApplication $@"
echo $prod
exec $prod
