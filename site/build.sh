#!/bin/sh
rm -rf /scratch/hps_site/
mvn clean site:site site:stage -DstagingDirectory=/scratch/hps_site/
