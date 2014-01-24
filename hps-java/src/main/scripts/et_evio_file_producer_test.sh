#!/bin/sh

# Script to stream an EVIO file onto an ET ring.
# You need to setup the path to JNI libraries yourself which are in hps-et-java/lib/[arch] .

# Single argument is name of EVIO file.
eviofile=$1
shift

# Classpath pointing to the hps-java jar.
classpath=${project.basedir}/target/${project.artifactId}-${project.version}-bin.jar

# Run it.
prod="java -classpath $classpath org.lcsim.hps.evio.EvioFileProducer -e ${eviofile} -f ETBuffer -host localhost -s 10000 -d 100 $@"
echo $prod
exec $prod
