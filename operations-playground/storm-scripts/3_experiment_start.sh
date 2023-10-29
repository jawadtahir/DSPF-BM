#!/usr/bin/env bash


# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

#export DELAY_COUNT=5
export DELAY_COUNT=9999999
export DELAY_LENGTH=1
export EVENTS_PER_WINDOW=5000
export KAFKA_BOOTSTRAP=kafka1:9092
export BENCHMARK_LENGTH="120"
export NUM_PRODUCERS=1
export NUM_STREAMS=2

# Start utils containers
echo "Starting utils containers..."
docker stack deploy --prune -c docker-compose-utils.yaml utils

#echo "Sleeping for 180s..."
#sleep 180s
##Node crash experiment
#echo "Removing container..."
#docker service rm kstreams_kstreams2
#
#echo "Sleeping for 150s..."
#sleep 150s
#
#echo "Redeploying container..."
#docker stack deploy --prune -c docker-compose-kstreams.yaml kstreams
#
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




