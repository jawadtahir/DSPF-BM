#!/usr/bin/env bash

for i in {1..2} ; do
    echo "run #$i"
    ./2_storm_start.sh
    sleep 10s
    ./3_experiment_start.sh
    echo "Sleeping for 120s"
    sleep 120s
done