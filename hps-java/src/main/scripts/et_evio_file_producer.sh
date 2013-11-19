#!/bin/sh

# Script to stream an EVIO file onto an ET ring.
# You need to setup the path to JNI libraries yourself which are in hps-et-java/lib/[arch] .

# Single argument is name of EVIO file.
eviofile=$1

# Classpath pointing to the hps-java jar.
classpath=${project.basedir}/target/hps-java-${project.version}-bin.jar

# Run it.
prod="java -classpath $classpath org.lcsim.hps.evio.EvioFileProducer $@"
echo $prod
exec $prod
