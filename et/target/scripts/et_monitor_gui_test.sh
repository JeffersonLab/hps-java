#!/bin/sh

#
# Run the ET GUI Monitor.
#

et=/local/celentano/eclipse_workspace/hps/et/target/hps-et-3.0.2-SNAPSHOT.jar
classpath=${et}:/local/celentano/eclipse_workspace/hps/et/jars/jlayout30.jar:/local/celentano/eclipse_workspace/hps/et/jars/jlm20.jar:/local/celentano/eclipse_workspace/hps/et/jars/jloox20.jar
monitor="java -classpath $classpath org.jlab.coda.et.monitorGui.Monitor"
echo "$monitor"
exec $monitor
