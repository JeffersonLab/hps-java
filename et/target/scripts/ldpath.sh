#!/bin/sh

if [[ `uname` == "Darwin" ]]; then
  export DYLD_LIBRARY_PATH=/local/celentano/eclipse_workspace/hps/et/lib/Darwin-x86_32/
  echo DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH
elif [[ `uname` == "Linux" ]]; then
  export LD_LIBRARY_PATH=/local/celentano/eclipse_workspace/hps/et/lib/Linux-x86_64/
  echo LD_LIBRARY_PATH=$LD_LIBRARY_PATH
else
  echo "ERROR: Unrecognized platform."
fi
