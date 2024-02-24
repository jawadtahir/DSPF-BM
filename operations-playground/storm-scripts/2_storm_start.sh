#!/usr/bin/env bash


CREATE_TOPIC_SCRIPT="/home/foobar/Downloads/kafka_2.13-3.6.0/bin/kafka-topics.sh"

# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

# Remove any previous Storm containers
echo "Removing any previous Storm containers..."
docker service rm storm_supervisor1 storm_supervisor2 storm_supervisor3
docker stack rm storm

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

# Sleep for 15 seconds
echo "Sleeping for 15 seconds..."
sleep 15s

echo "Creating topics..."
#${CREATE_TOPIC_SCRIPT} --topic click --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094
#${CREATE_TOPIC_SCRIPT} --topic update --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094
#${CREATE_TOPIC_SCRIPT} --topic output --create --partitions 3 --replication-factor 3 --bootstrap-server node6:9094

${CREATE_TOPIC_SCRIPT} --topic click --create  --bootstrap-server node1:9094 --replica-assignment 1001:1002:1003,1002:1001:1003,1003:1001:1002
${CREATE_TOPIC_SCRIPT} --topic update --create  --bootstrap-server node1:9094 --replica-assignment 1001:1002:1003,1002:1001:1003,1003:1001:1002
${CREATE_TOPIC_SCRIPT} --topic output --create  --bootstrap-server node1:9094 --replica-assignment 1001:1002:1003,1002:1001:1003,1003:1001:1002
${CREATE_TOPIC_SCRIPT} --topic lateOutput --create  --bootstrap-server node1:9094 --replica-assignment 1001:1002:1003,1002:1001:1003,1003:1001:1002


#${CREATE_TOPIC_SCRIPT} --topic click --create  --bootstrap-server node1:9094
#${CREATE_TOPIC_SCRIPT} --topic update --create  --bootstrap-server node1:9094
#${CREATE_TOPIC_SCRIPT} --topic output --create  --bootstrap-server node1:9094
#${CREATE_TOPIC_SCRIPT} --topic lateOutput --create  --bootstrap-server node1:9094

# Sleep for 10 seconds
echo "Sleeping for 10 seconds..."
sleep 10s

export NUM_STREAMS=1
# Start Storm containers
echo "Starting Storm containers..."
docker stack deploy --prune -c docker-compose-storm3.yaml storm

