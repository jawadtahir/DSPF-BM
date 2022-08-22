#!/usr/bin/env bash

docker_swarm_volume_prune()
{
  NODE_NAME=$1
  echo "SSH into ${NODE_NAME}"
  ssh -o StrictHostKeyChecking=no ubuntu@$NODE_NAME docker volume prune -f
}

NODE_LIST=("node1")



# Remove any previous KStreams containers
echo "Removing any previous monitoring containers..."
docker stack rm monitoring

echo "Sleeping for 10 seconds"
sleep 10s

## Remove any previous volumes
#echo "Removing any orphan volumes..."
##docker volume prune -f
#for i in "${NODE_LIST[@]}"; do
#  docker_swarm_volume_prune "$i"
#done

# Start monitoring containers
echo "Starting monitoring containers..."
docker stack deploy -c docker-compose-monitoring.yaml monitoring
