#!/bin/sh
java -classpath ${project.basedir}/target/${project.artifactId}-${project.version}-bin.jar org.lcsim.hps.evio.BasicEvioFileReader $@
