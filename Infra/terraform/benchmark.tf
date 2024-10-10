terraform {
  required_providers {
    opennebula = {
      source  = "opennebula/opennebula"
      version = "1.0.1"
    }
  }
}

provider "opennebula" {
  endpoint      = "http://xml-rpc.cloud.dis.cit.tum.de"
  flow_endpoint = "http://oneflow.cloud.dis.cit.tum.de"
  username      = var.ON_USERNAME
  password      = var.ON_PASSWD
  insecure      = true
}

resource "opennebula_virtual_machine" "flink" {
  count       = 4
  name        = "benchmark-flink-${count.index}"
  description = "Apache Flink cluster"
  cpu = 8
  vcpu = 8
  memory = 18432 #16 GiB
  group = "tahir-research"
  template_id = 118

  # tags = {
  #   "machine_count" = count.index
  # }

  disk {
    image_id = 97
    size = 30720
    target = "vda"
    driver = "qcow2"
  }
}

output "flink-output" {
  value = opennebula_virtual_machine.flink.*.ip
}


resource "opennebula_virtual_machine" "kafka" {
  count       = 3
  name        = "benchmark-kafka-${count.index}"
  description = "Kafka cluster"
  cpu = 8
  vcpu = 8
  memory = 18432 #16 GiB
  group = "tahir-research"
  template_id = 118

  # tags = {
  #   "machine_count" = count.index
  # }

  disk {
    image_id = 97
    size = 30720
    target = "vda"
    driver = "qcow2"
  }

}

output "kafka-output" {
  value = opennebula_virtual_machine.kafka.*.ip
}


resource "opennebula_virtual_machine" "hdfs" {
  count       = 1
  name        = "benchmark-hdfs-${count.index}"
  description = "HDFS machine"
  cpu = 8
  vcpu = 8
  memory = 18432 #16 GiB
  group = "tahir-research"
  template_id = 118

  # tags = {
  #   "machine_count" = count.index
  # }

  disk {
    image_id = 97
    size = 30720
    target = "vda"
    driver = "qcow2"
  }

}

output "hdfs-output" {
  value = opennebula_virtual_machine.hdfs.*.ip
}

resource "opennebula_virtual_machine" "storm" {
  count       = 4
  name        = "benchmark-storm-${count.index}"
  description = "Apache Storm cluster"
  cpu = 8
  vcpu = 8
  memory = 18432 #16 GiB
  group = "tahir-research"
  template_id = 118

  # tags = {
  #   "machine_count" = count.index
  # }

  disk {
    image_id = 97
    size = 30720
    target = "vda"
    driver = "qcow2"
  }

}
output "storm-output" {
  value = opennebula_virtual_machine.storm.*.ip
}

resource "opennebula_virtual_machine" "kstreams" {
  count       = 4
  name        = "benchmark-kstreams-${count.index}"
  description = "Kafka Stream cluster"
  cpu = 8
  vcpu = 8
  memory = 18432 #16 GiB
  group = "tahir-research"
  template_id = 118

  # tags = {
  #   "machine_count" = count.index
  # }

  disk {
    image_id = 97
    size = 30720
    target = "vda"
    driver = "qcow2"
  }

}

output "kstreams-output" {
  value = opennebula_virtual_machine.kstreams.*.ip
}

resource "opennebula_virtual_machine" "spark" {
  count       = 4
  name        = "benchmark-spark-${count.index}"
  description = "Apache Spark cluster"
  cpu = 8
  vcpu = 8
  memory = 18432 #16 GiB
  group = "tahir-research"
  template_id = 118

  # tags = {
  #   "machine_count" = count.index
  # }

  disk {
    image_id = 97
    size = 30720
    target = "vda"
    driver = "qcow2"
  }

}

output "spark-output" {
  value = opennebula_virtual_machine.spark.*.ip
}

resource "opennebula_virtual_machine" "utils" {
  count = 2
  name  = "benchmark-utils-${count.index}"
  description   = "Utils machine"
  cpu = 8
  vcpu = 8
  memory = 18432
  group  = "tahir-research"
  template_id = 118

  disk {
    image_id = 97
    size = 30720
    target = "vda"
    driver = "qcow2"
  }
}

output "utils-output" {
  value = opennebula_virtual_machine.utils.*.ip
}


resource "local_file" "ansible_inventory" {
  content = templatefile("ansible-inventory.tftpl",
    {
      #  ansible_group_flink = opennebula_virtual_machine.flink.*.tags.machine_count,
      flink_machines = opennebula_virtual_machine.flink.*.ip
      kafka_machines = opennebula_virtual_machine.kafka.*.ip
      hdfs_machines = opennebula_virtual_machine.hdfs.*.ip
      storm_machines = opennebula_virtual_machine.storm.*.ip
      spark_machines = opennebula_virtual_machine.spark.*.ip
      utils_machines = opennebula_virtual_machine.utils.*.ip
      kstreams_machine = opennebula_virtual_machine.kstreams.*.ip
    }
  )
  filename = "../ansible/inventory.cfg"
}
