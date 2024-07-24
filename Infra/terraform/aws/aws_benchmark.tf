terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region        = "us-east-1"
}

resource "aws_instance" "kafka" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-kafka-${count.index}"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
  count       = 3
}

resource "aws_instance" "utils" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-utils-0"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
}

resource "aws_instance" "utils2" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-utils-1"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
}


resource "aws_instance" "hdfs" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-hdfs-0"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
}



resource "aws_instance" "flink_jm" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-flink-0"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
}


resource "aws_instance" "flink_tm" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-flink-${count.index+1}"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
  count       = 3
}

resource "aws_instance" "kstreams" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-kafka-${count.index}"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
  count       = 4
}


resource "aws_instance" "nimbus" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-storm-0"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
}


resource "aws_instance" "storm_supervisor" {
  ami = "ami-080e1f13689e07408"
  instance_type = var.AWS_INSTANCE_TYPE
  associate_public_ip_address = "true"
  key_name = var.AWS_KEYNAME
  security_groups = [var.AWS_SECURITY_GROUP]
  subnet_id = var.AWS_SUBNET
  tags = {
    Name = "benchmark-storm-${count.index+1}"
  }
  root_block_device {
    volume_size = 8
    volume_type = "gp2"
  }
  count       = 3
}



#output "flink-output-external" {
#  value = aws_instance.flink.*.public_ip
#}
#
#output "flink-output-internal" {
#  value = aws_instance.flink.*.private_ip
#}

#resource "opennebula_virtual_machine" "kafka" {
#  count       = 3
#  name        = "benchmark-kafka-${count.index}"
#  description = "Kafka cluster"
#  cpu = 8
#  vcpu = 8
#  memory = 18432 #16 GiB
#  group = "tahir-research"
#  template_id = 118
#
#  # tags = {
#  #   "machine_count" = count.index
#  # }
#
#  disk {
#    image_id = 97
#    size = 30720
#    target = "vda"
#    driver = "qcow2"
#  }
#
#}
#
#output "kafka-output" {
#  value = opennebula_virtual_machine.kafka.*.ip
#}
#
#
#resource "opennebula_virtual_machine" "hdfs" {
#  count       = 1
#  name        = "benchmark-hdfs-${count.index}"
#  description = "HDFS machine"
#  cpu = 8
#  vcpu = 8
#  memory = 18432 #16 GiB
#  group = "tahir-research"
#  template_id = 118
#
#  # tags = {
#  #   "machine_count" = count.index
#  # }
#
#  disk {
#    image_id = 97
#    size = 30720
#    target = "vda"
#    driver = "qcow2"
#  }
#
#}
#
#output "hdfs-output" {
#  value = opennebula_virtual_machine.hdfs.*.ip
#}
#
#resource "opennebula_virtual_machine" "storm" {
#  count       = 4
#  name        = "benchmark-storm-${count.index}"
#  description = "Apache Storm cluster"
#  cpu = 8
#  vcpu = 8
#  memory = 18432 #16 GiB
#  group = "tahir-research"
#  template_id = 118
#
#  # tags = {
#  #   "machine_count" = count.index
#  # }
#
#  disk {
#    image_id = 97
#    size = 30720
#    target = "vda"
#    driver = "qcow2"
#  }
#
#}
#output "storm-output" {
#  value = opennebula_virtual_machine.storm.*.ip
#}
#
#resource "opennebula_virtual_machine" "kstreams" {
#  count       = 4
#  name        = "benchmark-kstreams-${count.index}"
#  description = "Kafka Stream cluster"
#  cpu = 8
#  vcpu = 8
#  memory = 18432 #16 GiB
#  group = "tahir-research"
#  template_id = 118
#
#  # tags = {
#  #   "machine_count" = count.index
#  # }
#
#  disk {
#    image_id = 97
#    size = 30720
#    target = "vda"
#    driver = "qcow2"
#  }
#
#}
#
#output "kstreams-output" {
#  value = opennebula_virtual_machine.kstreams.*.ip
#}
#
#resource "opennebula_virtual_machine" "spark" {
#  count       = 4
#  name        = "benchmark-spark-${count.index}"
#  description = "Apache Spark cluster"
#  cpu = 8
#  vcpu = 8
#  memory = 18432 #16 GiB
#  group = "tahir-research"
#  template_id = 118
#
#  # tags = {
#  #   "machine_count" = count.index
#  # }
#
#  disk {
#    image_id = 97
#    size = 30720
#    target = "vda"
#    driver = "qcow2"
#  }
#
#}
#
#output "spark-output" {
#  value = opennebula_virtual_machine.spark.*.ip
#}
#
#resource "opennebula_virtual_machine" "utils" {
#  count = 2
#  name  = "benchmark-utils-${count.index}"
#  description   = "Utils machine"
#  cpu = 8
#  vcpu = 8
#  memory = 18432
#  group  = "tahir-research"
#  template_id = 118
#
#  disk {
#    image_id = 97
#    size = 30720
#    target = "vda"
#    driver = "qcow2"
#  }
#}
#
#output "utils-output" {
#  value = opennebula_virtual_machine.utils.*.ip
#}
#
#
resource "local_file" "ansible_inventory" {
  content = templatefile("ansible-inventory.tftpl",
    {
      #  ansible_group_flink = opennebula_virtual_machine.flink.*.tags.machine_count,
      flink_jobman = aws_instance.flink_jm.private_ip
      flink_taskman = aws_instance.flink_tm.*.private_ip
      kafka_machines = aws_instance.kafka.*.private_ip
      hdfs = aws_instance.hdfs.private_ip
      nimbus = aws_instance.nimbus.private_ip
      storm_machines = aws_instance.storm_supervisor.*.private_ip
#      spark_machines = opennebula_virtual_machine.spark.*.ip
      utils = aws_instance.utils.private_ip
      utils2 = aws_instance.utils2.private_ip
      kstreams_machine = aws_instance.kstreams.*.private_ip
    }
  )
#  filename = "../../ansible/inventory.cfg"
  filename = "inventory.cfg"
}
