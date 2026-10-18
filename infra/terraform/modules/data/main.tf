locals {
  engines = var.enabled_rds ? { postgres = { engine = "postgres", user = "pgadmin", name = "platform" }, mariadb = { engine = "mariadb", user = "mariaadmin", name = "account_db" } } : {}
}
resource "aws_db_subnet_group" "rds" {
  count      = var.enabled_rds ? 1 : 0
  name       = "${var.prefix}-rds"
  subnet_ids = var.subnets
  tags       = { zone = "main" }
}
resource "aws_db_instance" "service" {
  for_each                    = local.engines
  identifier                  = "${var.prefix}-${each.key}"
  engine                      = each.value.engine
  engine_version              = each.key == "postgres" ? "17" : "11.8"
  instance_class              = "db.t4g.micro"
  allocated_storage           = 20
  max_allocated_storage       = 40
  storage_type                = "gp3"
  storage_encrypted           = true
  db_name                     = each.value.name
  username                    = each.value.user
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.rds[0].name
  vpc_security_group_ids      = [var.security_groups[each.key]]
  publicly_accessible         = false
  multi_az                    = false
  backup_retention_period     = 1
  deletion_protection         = true
  skip_final_snapshot         = false
  final_snapshot_identifier   = "${var.prefix}-${each.key}-final"
  tags                        = { zone = "main" }
}
resource "aws_elasticache_subnet_group" "redis" {
  count      = var.enabled_redis ? 1 : 0
  name       = "${var.prefix}-redis"
  subnet_ids = var.subnets
  tags       = { zone = "main" }
}
resource "aws_elasticache_user" "app" {
  count         = var.enabled_redis ? 1 : 0
  user_id       = "${var.prefix}-app"
  user_name     = "${var.prefix}-app"
  engine        = "redis"
  access_string = "on ~${var.prefix}:* +@read +@write +@connection -@dangerous +ping +eval +evalsha +script|load +script|exists"
  authentication_mode { type = "iam" }
  tags = { zone = "main" }
}
resource "aws_elasticache_user" "disabled_default" {
  count         = var.enabled_redis ? 1 : 0
  user_id       = "${var.prefix}-default"
  user_name     = "default"
  engine        = "redis"
  access_string = "off ~* -@all"
  authentication_mode { type = "no-password-required" }
  tags = { zone = "main" }
}
resource "aws_elasticache_user_group" "app" {
  count         = var.enabled_redis ? 1 : 0
  user_group_id = "${var.prefix}-app"
  engine        = "redis"
  user_ids      = [aws_elasticache_user.app[0].user_id, aws_elasticache_user.disabled_default[0].user_id]
  tags          = { zone = "main" }
}
resource "aws_elasticache_replication_group" "redis" {
  count                      = var.enabled_redis ? 1 : 0
  replication_group_id       = "${var.prefix}-redis"
  description                = "Private development Redis"
  engine                     = "redis"
  engine_version             = "7.1"
  node_type                  = "cache.t4g.micro"
  num_cache_clusters         = 1
  port                       = 6379
  subnet_group_name          = aws_elasticache_subnet_group.redis[0].name
  security_group_ids         = [var.security_groups.redis]
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  user_group_ids             = [aws_elasticache_user_group.app[0].user_group_id]
  tags                       = { zone = "main" }
}
resource "aws_msk_configuration" "kafka" {
  count             = var.enabled_msk ? 1 : 0
  name              = "${var.prefix}-kafka"
  kafka_versions    = ["3.9.x"]
  server_properties = <<-PROPERTIES
    auto.create.topics.enable=false
    default.replication.factor=2
    min.insync.replicas=2
  PROPERTIES
}
resource "aws_msk_cluster" "kafka" {
  count                  = var.enabled_msk ? 1 : 0
  cluster_name           = "${var.prefix}-kafka"
  kafka_version          = "3.9.x"
  number_of_broker_nodes = 2
  broker_node_group_info {
    instance_type   = "kafka.t3.small"
    client_subnets  = var.subnets
    security_groups = [var.security_groups.kafka]
    storage_info {
      ebs_storage_info { volume_size = 10 }
    }
  }
  configuration_info {
    arn      = aws_msk_configuration.kafka[0].arn
    revision = aws_msk_configuration.kafka[0].latest_revision
  }
  client_authentication {
    sasl { iam = true }
  }
  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }
  tags = { zone = "main" }
}
output "rds_endpoints" { value = { for k, v in aws_db_instance.service : k => v.address } }
output "rds_master_secret_arns" { value = { for k, v in aws_db_instance.service : k => v.master_user_secret[0].secret_arn } }
output "redis_endpoint" { value = try(aws_elasticache_replication_group.redis[0].primary_endpoint_address, "") }
output "redis_user" { value = try(aws_elasticache_user.app[0].user_name, "") }
output "redis_iam_resources" { value = var.enabled_redis ? [aws_elasticache_replication_group.redis[0].arn, aws_elasticache_user.app[0].arn] : [] }
output "kafka_bootstrap" { value = try(aws_msk_cluster.kafka[0].bootstrap_brokers_sasl_iam, "") }
output "kafka_arn" { value = try(aws_msk_cluster.kafka[0].arn, "") }
