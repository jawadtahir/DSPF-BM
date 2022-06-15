#!/usr/bin/env bash

docker_swarm_volume_prune()
{
  NODE_NAME=$1
  echo "SSH into ${NODE_NAME}"
  ssh -o StrictHostKeyChecking=no ubuntu@$NODE_NAME docker volume prune -f
}

#NODE_LIST=("kafka1" "kafka2" "kafka3" "kafka4" "kafka5" "kafka6" "kafka7")
NODE_LIST=("node6" "node7" "node8" "node9" "node10" "node11" "node12")

CREATE_TOPIC_SCRIPT="/home/foobar/Downloads/kafka_2.13-2.6.0/bin/kafka-topics.sh"

#for i in "${NODE_LIST[@]}"; do
#  docker_swarm_volume_prune "$i"
#done

# Remove any previous KStreams containers
echo "Removing any previous KStreams containers..."
docker stack rm kstreams

# Remove any previous Kafka containers
echo "Removing any previous Kafka containers..."
docker stack rm kafka

# Sleep for 5 seconds
echo "Sleeping for 5 seconds..."
sleep 5s



# Start kafka containers
echo "Starting Kafka containers..."
#docker-compose -f docker-compose-kafka.yaml up -d
docker stack deploy --prune -c docker-compose-kafka.yaml kafka

# Sleep for 20 seconds
echo "Sleeping for 20 seconds..."
sleep 20s

echo "Creating topics..."
${CREATE_TOPIC_SCRIPT} --topic click --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094
${CREATE_TOPIC_SCRIPT} --topic update --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094
${CREATE_TOPIC_SCRIPT} --topic output --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094


# Sleep for 10 seconds
echo "Sleeping for 10 seconds..."
sleep 10s

# Start KStreams containers
echo "Starting KStreams containers..."
#docker-compose -f docker-compose-kstreams.yaml up --build -d
docker stack deploy --prune -c docker-compose-kstreams.yaml kstreams

