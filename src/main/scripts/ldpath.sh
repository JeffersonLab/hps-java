#!/bin/sh

# Get the OS details.
os=`uname -a`

# Set the base shared lib dir.
lib_dir=${project.basedir}/src/main/resources/lib

# Set the load library path by OS.
if [[ $os == *Darwin* ]]; then
    # Set path for 32-bit OSX, which is also used for 64-bit systems.
    export DYLD_LIBRARY_PATH=${lib_dir}/Darwin-x86_32/
    echo DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH
elif [[ $os == *Linux* ]]; then    
    if [[ $os == *x86_64* ]]; then
        # Set path for 64-bit Linux.
        export LD_LIBRARY_PATH=${lib_dir}/Linux-x86_64/
    else
        # Set path for 32-bit Linux.
        export LD_LIBRARY_PATH=${lib_dir}/Linux-x86_32/
    fi    
    echo LD_LIBRARY_PATH=$LD_LIBRARY_PATH
else
    echo "ERROR: Unrecognized platform " $os 
fi