mvn clean install -DskipTests -T4 &> build.log &
tail -f build.log
