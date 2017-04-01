#!/bin/sh
det=${1##*detectors/}
det=${det%%/}
java \
-cp ../detector-model/target/hps-detector-model-*SNAPSHOT-bin.jar \
org.hps.detector.DetectorConverter \
-f lcdd \
-i detectors/$det/compact.xml \
-o $det.lcdd

