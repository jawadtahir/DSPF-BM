#!/usr/bin/env bash


# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

#echo "Sleeping for 15 seconds"
#sleep 15s
#
## Remove any previous volumes
#echo "Removing any orphan volumes..."
##docker volume prune -f
#for i in "${NODE_LIST[@]}"; do
#  docker_swarm_volume_prune "$i"
#done

export DELAY_COUNT=1
export DELAY_LENGTH=1

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

echo "Sleeping for 480s (180+150+150)"
sleep 480s

echo "Removing datagen container..."
docker service rm utils_datagen




