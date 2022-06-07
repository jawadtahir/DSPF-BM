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
echo "Removing datagen container..."
docker service rm utils_datagen

#echo "Sleeping for 10 seconds"
#sleep 10s
#
## Remove any previous volumes
#echo "Removing any orphan volumes..."
##docker volume prune -f
#for i in "${NODE_LIST[@]}"; do
#  docker_swarm_volume_prune "$i"
#done
## Start utils containers
#echo "Starting utils containers..."
#docker-compose -f docker-compose-utils.yaml up --build -d




