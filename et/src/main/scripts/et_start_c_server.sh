#!/bin/sh

server="${project.basedir}/bin/Linux-x86_64/et_start"
echo "$server"
exec $server $@
