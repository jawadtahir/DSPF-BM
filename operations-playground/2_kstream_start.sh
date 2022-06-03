#!/usr/bin/env bash

docker_swarm_volume_prune()
{
  NODE_NAME=$1
  echo "SSH into ${NODE_NAME}"
  ssh -o StrictHostKeyChecking=no ubuntu@$NODE_NAME docker volume prune -f
}

NODE_LIST=("kafka1" "kafka2" "kafka3" "kafka4" "kafka5" "kafka6" "kafka7")
#NODE_LIST=("node3" "node4" "node5")

#for i in "${NODE_LIST[@]}"; do
#  docker_swarm_volume_prune "$i"
#done

# Remove any previous KStreams containers
echo "Removing any previous KStreams containers..."
docker stack rm kstreams

# Remove any previous Kafka containers
echo "Removing any previous Kafka containers..."
docker stack rm kafka

echo "Sleeping for 15 seconds"
sleep 15s

# Remove any previous volumes
echo "Removing any orphan volumes..."
#docker volume prune -f
for i in "${NODE_LIST[@]}"; do
  docker_swarm_volume_prune "$i"
done

# Start kafka containers
echo "Starting Kafka containers..."
#docker-compose -f docker-compose-kafka.yaml up -d
docker stack deploy -c docker-compose-kafka.yaml kafka

## Sleep for 30 seconds
#echo "Sleeping for 30 seconds..."
#sleep 30s
#
## Start KStreams containers
#echo "Starting KStreams containers..."
##docker-compose -f docker-compose-kstreams.yaml up --build -d
#docker stack deploy -c docker-compose-kstreams.yaml kstreams

