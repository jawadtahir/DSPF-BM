#!/usr/bin/env bash

docker_swarm_volume_prune()
{
  NODE_NAME=$1
  echo "SSH into ${NODE_NAME}"
  ssh -o StrictHostKeyChecking=no ubuntu@$NODE_NAME docker volume prune -f
}

NODE_LIST=("node1" "node2" "node3" "node4" "node5" "kafka1" "kafka2" "kafka3" "kafka4" "kafka5" "kafka6" "storm1" "storm2" "storm3" "storm4" "storm5")

#for i in "${NODE_LIST[@]}"; do
#  docker_swarm_volume_prune "$i"
#done

# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker-compose -f docker-compose-utils.yaml rm -f -s -v

echo "Sleeping for 15 seconds"
sleep 15s

# Remove any previous volumes
echo "Removing any orphan volumes..."
docker volume prune -f

export DELAY_COUNT=10

# Start utils containers
echo "Starting utils containers..."
docker-compose -f docker-compose-utils.yaml up --build -d




