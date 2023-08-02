#!/usr/bin/env bash

#docker_swarm_volume_prune()
#{
#  NODE_NAME=$1
#  echo "SSH into ${NODE_NAME}"
#  ssh -o StrictHostKeyChecking=no ubuntu@$NODE_NAME docker volume prune -f
#}

#NODE_LIST=("kafka1" "kafka2" "kafka3" "kafka4" "kafka5" "kafka6" "kafka7")
#NODE_LIST=("node6" "node7" "node8" "node9" "node10" "node11" "node12")

CREATE_TOPIC_SCRIPT="/home/foobar/Downloads/kafka_2.13-2.6.0/bin/kafka-topics.sh"

#for i in "${NODE_LIST[@]}"; do
#  docker_swarm_volume_prune "$i"
#done

# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

# Remove any previous Flink containers
echo "Removing any previous Flink containers..."
docker stack rm flink

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
#${CREATE_TOPIC_SCRIPT} --topic click --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094
#${CREATE_TOPIC_SCRIPT} --topic update --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094
#${CREATE_TOPIC_SCRIPT} --topic output --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094

${CREATE_TOPIC_SCRIPT} --topic click --create  --bootstrap-server node1:9094 --replica-assignment 1001:1002:1003,1002:1001:1003,1003:1001:1002
${CREATE_TOPIC_SCRIPT} --topic update --create  --bootstrap-server node1:9094 --replica-assignment 1001:1002:1003,1002:1001:1003,1003:1001:1002
${CREATE_TOPIC_SCRIPT} --topic output --create  --bootstrap-server node1:9094 --replica-assignment 1001:1002:1003,1002:1001:1003,1003:1001:1002


# Sleep for 10 seconds
echo "Sleeping for 10 seconds..."
sleep 10s

# Start Storm containers
echo "Starting Storm containers..."
docker stack deploy --prune -c docker-compose-flink.yaml flink

