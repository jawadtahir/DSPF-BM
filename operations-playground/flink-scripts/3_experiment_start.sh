#!/usr/bin/env bash


# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

export DELAY_COUNT=1
export DELAY_LENGTH=2

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

echo "Sleeping for 540s (180+180+180)"
sleep 540s

echo "Removing datagen container..."
docker service rm utils_datagen




