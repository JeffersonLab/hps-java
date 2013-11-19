#!/bin/sh
java -classpath ${project.basedir}/target/hps-java-${project.version}-bin.jar org.lcsim.hps.evio.BasicEvioFileReader $@
