#!/usr/bin/env bash

docker_swarm_volume_prune()
{
  NODE_NAME=$1
  echo "SSH into ${NODE_NAME}"
  ssh -o StrictHostKeyChecking=no ubuntu@$NODE_NAME docker volume prune -f
}

NODE_LIST=("node1")

#for i in "${NODE_LIST[@]}"; do
#  docker_swarm_volume_prune "$i"
#done

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

export DELAY_COUNT=3

# Start utils containers
echo "Starting utils containers..."
docker stack deploy -c docker-compose-utils.yaml utils

echo "Sleeping for 360s"
sleep 360s

echo "Removing datagen container..."
docker service rm utils_datagen




