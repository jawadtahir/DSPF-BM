#!/usr/bin/env bash

# Remove any previous analysis containers
echo "Removing any previous analysis containers..."
docker stack rm analysis


# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

#export DELAY_COUNT=99999
export DELAY_COUNT=9999999999
export DELAY_LENGTH=1
export EVENTS_PER_WINDOW=500
export KAFKA_BOOTSTRAP=kafka1:9092
export BENCHMARK_LENGTH="210"
export NUM_PRODUCERS=1
export NUM_STREAMS=2

# Start utils containers
echo "Starting utils containers..."
docker stack deploy --prune -c docker-compose-utils.yaml utils

#echo "Sleeping for 60s..."
#sleep 60s
##Node crash experiment
#echo "Removing container..."
#docker service rm flink_taskmanager2
#
#echo "Sleeping for 60s..."
#sleep 60s
#
#export PG=e1
##export PG=a1
#
#echo "Redeploying container..."
#docker stack deploy --prune -c docker-compose-flink.yaml flink
#
#echo "Sleeping for 150s..."
#sleep 150s
#
#echo "Removing datagen container..."
#docker service rm utils_datagen

#echo "Sleeping for 180s (60+30+30)"
#sleep 180s
#
#echo "Removing datagen container..."
#docker service rm utils_datagen

#echo "Sleeping for 10s"
#sleep 10s
#
#echo "Running analysis scripts"
#docker stack deploy --prune -c docker-compose-analysis.yaml analysis





