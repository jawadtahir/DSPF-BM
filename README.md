# PGVal: Evaluate fault tolerance of Stream Processing Systems.

PGVal is a benchmarking system for stream processing systems (SPSs). 
PGVal benchmarks SPSs for correctness and performance. 
For details, please see the extended paper in `Docs` folder.

This ReadMe file explains the steps to performs benchmarks. To run benchmarks we need three things.

1. Infrastructure
2. Set up
3. Benchmark scripts


All components of the benchmark are deployed on a cluster of VMs. 
We provide terraform scripts to automate VM creation for OpenNebula and AWS (work in progress). 
These VMs need to be configured with the required packages and softwares. 
To this end, we provide Ansible scripts to automate the configuration. 
Once VMs are set up, you can run benchmark scripts.    

## Infrastructure
If you already have VMs that are connected on a network, you can skip this step and read set up instruction. 
We provide Terraform scripts for OpenNebula and AWS in `Infra/terraform` folder.
```shell
cd infra/terraform
terraform init
terraform plan
terraform apply
```


## Set up
We provide Ansible scripts to automate the set-up process in `Infra/ansible`. 
If you ran terraform scripts, an `inventory.cfg` will be created automatically. 
Otherwise, create an `inventory.cfg` as per given sample. After that run
```shell
cd infra/ansible
ansible-playbook -c ssh -i inventory.cfg setup-machines.yaml
```
The script install and configure required packages and softwares, such as HDFS, Java, Docker, and node_exporter.

## Benchmarking
We first need to run monitoring services (Prometheus and Grafana) to visualize the results.

```shell
cd operations-playground
./1_monitoring_start.sh
```
### Setting up the dashboard
* Open browser and head over to Grafana at `<utilsIP>:4300` (see `inventory.cfg` in `Infra/ansible`)
* Set up a new data source; URL `http://prometheus:9090`, Scrape interval `1s`
* Import the dashboard from `operations-playground/dashboard/dashboard.json`.


We provide scripts to benchmark Kafka Streams, Apache Storm, and Apache Flink in `kstreams-scripts`, `storm-scripts`, and `flink-scripts` folders, respectively. 
Each folder contains 2 scripts.
Run `./2_<SPS>_start.sh` script. 
The script deploys Apache Kafka cluster, creates topics, and deploys the specified SPS on a Docker Swarm cluster. 
After that, run
```shell
./3_experiment_start.sh
```
The experiment will run and can be observed in Grafana dashboard.
To configure experiments, please see wiki.

# CLEAN UP



## Requirements

### Hardware
We set up our machines on our cluster running OpenNebula. However, you can run it with your AWS account as well. AWS Terraform file is under development and will be provided later.
### Software
```
Terraform
Ansible
Docker
direnv
AWS CLI
```

## Set up infrastructure

Enable local SSH on HDFS machine
```shell
ssh-keygen -t rsa
cat .ssh/id_rsa.pub >> .ssh/authorized_keys
```

```shell
cd infra/terraform
terraform init
terraform plan
terraform apply
cd ../ansible
ansible-playbook -c ssh -i inventory.cfg setup-machines.yaml
```
* Go to `<hdfsIP>:9870`
* Create `/tmp/flink-savepoints-directory` folder and make it writeable for all

## Set up grafana dashboard 

```shell
cd ../../operations-plyground
./1_monitoring_start
```
* Open browser and head over to `<utilsIP>:4300` (see `inventory.cfg` in `Infra/ansible`)
* Set up data source; URL `http://prometheus:9090`, Scrape interval `1s`
* Import the dashboard from `operations-playground/dashboard/dashboard.json`.
