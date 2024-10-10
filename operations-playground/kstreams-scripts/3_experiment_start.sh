#!/usr/bin/env bash


# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

#export DELAY_COUNT=60
export DELAY_COUNT=3
export DELAY_LENGTH=1
export EVENTS_PER_WINDOW=500
export KAFKA_BOOTSTRAP=kafka1:9092
export BENCHMARK_LENGTH="210"
export NUM_PRODUCERS=1
export NUM_STREAMS=2
export UPDATE_DISTRIBUTION=10

# Start utils containers
echo "Starting utils containers..."
docker stack deploy --prune -c docker-compose-utils.yaml utils

#echo "Sleeping for 90s..."
#sleep 90s
##Node crash experiment
#echo "Removing container..."
#docker service rm kstreams_kstreams2
##
#echo "Sleeping for 60s..."
#sleep 60s
##
##export PROCESSING_GUARANTEE="at_least_once"
#export PROCESSING_GUARANTEE="exactly_once_v2"
##
#echo "Redeploying container..."
#docker stack deploy --prune -c docker-compose-kstreams.yaml kstreams
##
#echo "Sleeping for 150s..."
#sleep 150s
#
#echo "Removing datagen container..."
#docker service rm utils_datagen

#echo "Sleeping for 480s (180+150+150)"
#sleep 480s
#
#echo "Removing datagen container..."
#docker service rm utils_datagen




