#!/usr/bin/env bash

for i in {1..3} ; do
    echo "run #$i"
    ./2_kstream_start.sh
    sleep 10s
    ./3_experiment_start.sh
    sleep 10s
done