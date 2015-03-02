#!/bin/sh

# Set the load library path.
. ${project.build.directory}/scripts/ldpath.sh

# First argument, with no command switch, is name of EVIO file.
eviofile=$1

# Subsequent arguments are appended to the end of the command.
shift

# Classpath pointing to the hps-evio jar.
classpath=${project.build.directory}/${project.artifactId}-${project.version}-bin.jar

# Run the file producer, sending any additional arguments to the command.
#prod="java -classpath $classpath org.hps.evio.EvioFileProducer -e ${eviofile} -f ETBuffer -host localhost -s 10000 -d 100 $@"
prod="java -classpath $classpath org.hps.record.evio.EvioFileProducer -e ${eviofile} -f ETBuffer -host localhost -s 100000 $@"
echo $prod
exec $prod
