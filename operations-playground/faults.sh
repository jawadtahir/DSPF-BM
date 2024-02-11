#!/usr/bin/env bash


echo 'tc qdisc add dev eth0 root netem loss 20%'
echo $(date)
sudo tc qdisc add dev eth0 root netem loss 20%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem corrupt 20%'
echo $(date)
sudo tc qdisc add dev eth0 root netem corrupt 20%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem duplicate 20%'
echo $(date)
sudo tc qdisc add dev eth0 root netem duplicate 20%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'sudo tc qdisc add dev eth0 root netem reorder 20% delay 5ms'
echo $(date)
sudo sudo tc qdisc add dev eth0 root netem reorder 20% delay 5ms
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

##############################40

echo 'tc qdisc add dev eth0 root netem loss 40%'
echo $(date)
sudo tc qdisc add dev eth0 root netem loss 40%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem corrupt 40%'
echo $(date)
sudo tc qdisc add dev eth0 root netem corrupt 40%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem duplicate 40%'
echo $(date)
sudo tc qdisc add dev eth0 root netem duplicate 40%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'sudo tc qdisc add dev eth0 root netem reorder 40% delay 5ms'
echo $(date)
sudo sudo tc qdisc add dev eth0 root netem reorder 40% delay 5ms
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

##############################60

echo 'tc qdisc add dev eth0 root netem loss 60%'
echo $(date)
sudo tc qdisc add dev eth0 root netem loss 60%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem corrupt 60%'
echo $(date)
sudo tc qdisc add dev eth0 root netem corrupt 60%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem duplicate 60%'
echo $(date)
sudo tc qdisc add dev eth0 root netem duplicate 60%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'sudo tc qdisc add dev eth0 root netem reorder 60% delay 5ms'
echo $(date)
sudo sudo tc qdisc add dev eth0 root netem reorder 60% delay 5ms
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

##############################80

echo 'tc qdisc add dev eth0 root netem loss 80%'
echo $(date)
sudo tc qdisc add dev eth0 root netem loss 80%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem corrupt 80%'
echo $(date)
sudo tc qdisc add dev eth0 root netem corrupt 80%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem duplicate 80%'
echo $(date)
sudo tc qdisc add dev eth0 root netem duplicate 80%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'sudo tc qdisc add dev eth0 root netem reorder 80% delay 5ms'
echo $(date)
sudo sudo tc qdisc add dev eth0 root netem reorder 80% delay 5ms
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

##############################95

echo 'tc qdisc add dev eth0 root netem loss 95%'
echo $(date)
sudo tc qdisc add dev eth0 root netem loss 95%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem corrupt 95%'
echo $(date)
sudo tc qdisc add dev eth0 root netem corrupt 95%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'tc qdisc add dev eth0 root netem duplicate 95%'
echo $(date)
sudo tc qdisc add dev eth0 root netem duplicate 95%
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s

echo 'sudo tc qdisc add dev eth0 root netem reorder 95% delay 5ms'
echo $(date)
sudo sudo tc qdisc add dev eth0 root netem reorder 95% delay 5ms
sleep 60s
sudo tc qdisc del dev eth0 root
echo $(date)

sleep 5s


echo 'stress-ng --vm 1200 --vm-bytes 100% --vm-method all  -t 60s'
echo $(date)
stress-ng --vm 1200 --vm-bytes 100% --vm-method all  -t 60s
echo $(date)

sleep 5s

echo 'stress-ng --cpu 12 -t 60s'
echo $(date)
stress-ng --cpu 12 -t 60s
echo $(date)