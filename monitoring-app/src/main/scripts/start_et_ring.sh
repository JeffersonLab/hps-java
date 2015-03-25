#!/bin/sh

# Setup the classpath.
classpath=${project.build.directory}/${project.artifactId}-${project.version}-bin.jar

# Set variable for buffer file.
buffer_file=ETBuffer

# Delete buffer file if it exists already.
if [[ -e $buffer_file ]]; then
    rm $buffer_file              
fi

# Start the ET ring, sending any script arguments to the end of the command.
server="java -Xmx1024m -classpath $classpath org.jlab.coda.et.apps.StartEt -f $buffer_file -s 200000 -v $@" 
echo "$server"
exec $server
