#!/usr/bin/env bash


# Remove any previous utils containers
echo "Removing any previous utils containers..."
docker stack rm utils

#export DELAY_COUNT=5
export DELAY_COUNT=999
export DELAY_LENGTH=1
export EVENTS_PER_WINDOW=500
export KAFKA_BOOTSTRAP=kafka1:9092
export BENCHMARK_LENGTH="210"
export NUM_PRODUCERS=1
export NUM_STREAMS=1

# Start utils containers
echo "Starting utils containers..."
docker stack deploy --prune -c docker-compose-utils.yaml utils

#echo "Sleeping for 90s..."
#sleep 90s
##Node crash experiment
#echo "Removing container..."
#docker service rm storm_supervisor2
#
#echo "Sleeping for 60s..."
#sleep 60s
#
#echo "Redeploying container..."
#docker service create --mount type=bind,src="/home/ubuntu/csvmetrics",target="/data/csvmetrics" --config source=storm_storm_config,target=/conf/storm.yaml --env COMMAND="storm supervisor" --replicas 1 --constraint node.labels.vmname==supervisor2 --hostname supervisor2 --network infra --name storm_supervisor2  jawadtahir/storm:latest
#
#echo "Sleeping for 150s..."
#sleep 150s
#
#echo "Removing datagen container..."
#docker service rm utils_datagen

#echo "Sleeping for 480s (180+150+150)"
#sleep 480s
#
#echo "Removing datagen container..."
#docker service rm utils_datagen




