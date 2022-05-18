#! /bin/sh

# shellcheck disable=SC2164
cd jmx-exporter
nohup java -jar jmx_prometheus_httpserver.jar 12345 config.yaml &

cd ../kafka-topology

java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=52923 -Djava.rmi.server.hostname=localhost  -jar kafka-streams-topology.jar -appid kstreams-eventcount -kafka $KAFKA_BOOTSTRAP -input $INPUT_TOPIC -output $OUTPUT_TOPIC -guarantee $PROCESSING_GUARANTEE -standby STANDBY