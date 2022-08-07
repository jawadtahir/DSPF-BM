#! /bin/sh

# shellcheck disable=SC2164
cd /jmx_reporter
nohup java -jar jmx_prometheus_httpserver.jar 12345 config.yaml &

#cd ../kafka-topology

$COMMAND