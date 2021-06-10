iteration=$1
tag=$2

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters"
    exit 1
    fi

java -cp distribution/target/hps-distribution-5.1-SNAPSHOT-bin.jar org.hps.detector.DetectorConverter -f lcdd -i detector-data/detectors/HPS_${tag}_$iteration/compact.xml -o detector-data/detectors/HPS_${tag}_$iteration/HPS_${tag}_$iteration.lcdd
echo "name: HPS_${tag}_$iteration" > detector-data/detectors/HPS_${tag}_$iteration/detector.properties

cd detector-data
mvn -T 4 -DskipTests=true
cd ..
cd distribution
mvn -T 4 -DskipTests=true
cd ..

