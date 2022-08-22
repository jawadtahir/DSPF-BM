#!/usr/bin/env bash


# Remove any previous KStreams containers
echo "Removing any previous monitoring containers..."
docker stack rm monitoring

echo "Sleeping for 10 seconds"
sleep 10s

# Start monitoring containers
echo "Starting monitoring containers..."
docker stack deploy -c docker-compose-monitoring.yaml monitoring
