#!/bin/sh

classpath=/local/celentano/eclipse_workspace/hps/monitoring-app/target/hps-monitoring-app-3.0.2-SNAPSHOT-bin.jar
prod="java -Xmx1024m -classpath "$classpath" org.hps.monitoring.MonitoringApplication $@"
# -c 100"
echo $prod
exec $prod
