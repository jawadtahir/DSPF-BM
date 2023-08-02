# DSPS-BM: SPS benchmarking under failures

##Requirements

###Hardware
We set up our machines on our cluster running OpenNebula. However, you can run it with your AWS account as well. AWS Terraform file is under development and will be provided later.
###Software
```
Terraform
Ansible
Docker
direnv
```

##Set up infrastructure

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
* Import the dashboard.

#Old README.md (obsolete)
## TODO: Remove

This repository provides playgrounds to quickly and easily explore [Apache Flink](https://flink.apache.org)'s features.

The playgrounds are based on [docker-compose](https://docs.docker.com/compose/) environments.
Each subfolder of this repository contains the docker-compose setup of a playground, except for the `./docker` folder which contains code and configuration to build custom Docker images for the playgrounds.

## Available Playgrounds

Currently, the following playgrounds are available:

* The **Flink Operations Playground** (in the `operations-playground` folder) lets you explore and play with Flink's features to manage and operate stream processing jobs. You can witness how Flink recovers a job from a failure, upgrade and rescale a job, and query job metrics. The playground consists of a Flink cluster, a Kafka cluster and an example 
Flink job. The playground is presented in detail in
["Flink Operations Playground"](https://ci.apache.org/projects/flink/flink-docs-release-1.11/try-flink/flink-operations-playground.html), which is part of the _Try Flink_ section of the Flink documentation.

* The **Table Walkthrough** (in the `table-walkthrough` folder) shows to use the Table API to build an analytics pipeline that reads streaming data from Kafka and writes results to MySQL, along with a real-time dashboard in Grafana. The walkthrough is presented in detail in ["Real Time Reporting with the Table API"](https://ci.apache.org/projects/flink/flink-docs-release-1.11/try-flink/table_api.html), which is part of the _Try Flink_ section of the Flink documentation.

* The **PyFlink Walkthrough** (in the `pyflink-walkthrough` folder) provides a complete example that uses the Python API, and guides you through the steps needed to run and manage Pyflink Jobs. The pipeline used in this walkthrough reads data from Kafka, performs aggregations, and writes results to Elasticsearch that are visualized with Kibana. This walkthrough is presented in detail in the [pyflink-walkthrough README](pyflink-walkthrough).

## About

Apache Flink is an open source project of The Apache Software Foundation (ASF).

Flink is distributed data processing framework with powerful stream and batch processing capabilities.
Learn more about Flink at [http://flink.apache.org/](https://flink.apache.org/)
