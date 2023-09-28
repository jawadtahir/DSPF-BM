#!/usr/bin/env bash

# Remove any previous analysis containers
echo "Removing any previous analysis containers..."
docker stack rm analysis


# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

export DELAY_COUNT=1000
export DELAY_LENGTH=1
export EVENTS_PER_WINDOW=5000
export KAFKA_BOOTSTRAP=kafka1:9092
export SHORT_DATA=""

# Start utils containers
echo "Starting utils containers..."
docker stack deploy --prune -c docker-compose-utils.yaml utils

#echo "Sleeping for 180s..."
#sleep 180s
##Node crash experiment
#echo "Removing container..."
#docker service rm flink_taskmanager2
#
#echo "Sleeping for 150s..."
#sleep 150s
#
#echo "Redeploying container..."
#docker stack deploy --prune -c docker-compose-flink.yaml flink
#
#echo "Sleeping for 150s..."
#sleep 150s
#
#echo "Removing datagen container..."
#docker service rm utils_datagen

echo "Sleeping for 50s (30+10+10)"
sleep 50s

echo "Removing datagen container..."
docker service rm utils_datagen

echo "Sleeping for 10s"
sleep 10s

echo "Running analysis scripts"
docker stack deploy --prune -c docker-compose-analysis.yaml analysis





