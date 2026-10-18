variable "prefix" { type = string }
variable "enabled" { type = bool }
variable "enable_public_ingress" { type = bool }
variable "enable_endpoints" { type = bool }
variable "certificate_arn" { type = string }
variable "services" { type = map(object({ zone = string, group = string, port = number, database = string, engine = string })) }
variable "zones" { type = map(object({ vpc_id = string, app_subnets = list(string) })) }
variable "app_sgs" { type = map(string) }
variable "public_subnets" { type = list(string) }
variable "images" { type = map(string) }
variable "secret_names" { type = map(string) }
variable "rds_endpoints" { type = map(string) }
variable "redis_endpoint" { type = string }
variable "redis_user" { type = string }
variable "redis_iam_resources" { type = list(string) }
variable "kafka_bootstrap" { type = string }
variable "kafka_arn" { type = string }
variable "report_bucket_arn" { type = string }
locals {
  active            = var.enabled ? var.services : {}
  database_services = { for k, v in local.active : k => v if v.database != "" }
}
resource "aws_ecs_cluster" "zone" {
  for_each = var.enabled ? toset(keys(var.zones)) : toset([])
  name     = "${var.prefix}-${each.key}"
  tags     = { zone = each.key }
}
resource "aws_cloudwatch_log_group" "service" {
  for_each          = local.active
  name              = "/${var.prefix}/${each.key}"
  retention_in_days = 7
  tags              = { zone = each.value.zone }
}
data "aws_iam_policy_document" "assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}
data "aws_secretsmanager_secret" "service" {
  for_each = local.database_services
  name     = lookup(var.secret_names, each.key, "UNCONFIGURED")
}
resource "aws_iam_role" "execution" {
  for_each           = local.active
  name               = "${var.prefix}-${each.key}-execution"
  assume_role_policy = data.aws_iam_policy_document.assume.json
  tags               = { zone = each.value.zone }
}
resource "aws_iam_role_policy_attachment" "execution" {
  for_each   = local.active
  role       = aws_iam_role.execution[each.key].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}
resource "aws_iam_role_policy" "secret" {
  for_each = local.database_services
  role     = aws_iam_role.execution[each.key].id
  policy   = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = [data.aws_secretsmanager_secret.service[each.key].arn] }] })
}
resource "aws_iam_role" "task" {
  for_each           = local.active
  name               = "${var.prefix}-${each.key}-task"
  assume_role_policy = data.aws_iam_policy_document.assume.json
  tags               = { zone = each.value.zone }
}
resource "aws_iam_role_policy" "redis" {
  for_each = { for k, v in local.active : k => v if v.zone != "mock" }
  role     = aws_iam_role.task[each.key].id
  policy   = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = ["elasticache:Connect"], Resource = var.redis_iam_resources }] })
}
resource "aws_iam_role_policy" "kafka" {
  for_each = local.database_services
  role     = aws_iam_role.task[each.key].id
  policy = jsonencode({ Version = "2012-10-17", Statement = [
    { Effect = "Allow", Action = ["kafka-cluster:Connect", "kafka-cluster:DescribeCluster", "kafka-cluster:WriteDataIdempotently"], Resource = [var.kafka_arn] },
    { Effect = "Allow", Action = ["kafka-cluster:DescribeTopic", "kafka-cluster:ReadData", "kafka-cluster:WriteData"], Resource = ["${replace(var.kafka_arn, ":cluster/", ":topic/")}/*"] },
    { Effect = "Allow", Action = ["kafka-cluster:DescribeGroup", "kafka-cluster:AlterGroup"], Resource = ["${replace(var.kafka_arn, ":cluster/", ":group/")}/*"] }
  ] })
}
resource "aws_iam_role_policy" "reports" {
  for_each = var.enabled ? { "wealth-report" = var.services["wealth-report"] } : {}
  role     = aws_iam_role.task[each.key].id
  policy   = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = ["s3:GetObject", "s3:PutObject"], Resource = ["${var.report_bucket_arn}/*"] }] })
}
resource "aws_ecs_task_definition" "service" {
  for_each                 = local.active
  family                   = "${var.prefix}-${each.key}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.execution[each.key].arn
  task_role_arn            = aws_iam_role.task[each.key].arn
  runtime_platform {
    cpu_architecture        = "ARM64"
    operating_system_family = "LINUX"
  }
  container_definitions = jsonencode([{
    name         = each.key, image = lookup(var.images, each.key, "UNCONFIGURED"), essential = true,
    portMappings = [{ containerPort = each.value.port, protocol = "tcp" }],
    environment = concat([
      { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
      { name = "JAVA_TOOL_OPTIONS", value = "-XX:MaxRAMPercentage=65" },
      { name = "KAFKA_BOOTSTRAP_SERVERS", value = var.kafka_bootstrap },
      { name = "REDIS_HOST", value = var.redis_endpoint },
      { name = "REDIS_PORT", value = "6379" },
      { name = "REDIS_IAM_USER", value = var.redis_user }
    ], each.value.database != "" ? [{ name = "DB_URL", value = "jdbc:${each.value.engine}://${lookup(var.rds_endpoints, each.value.engine, "UNCONFIGURED")}:${each.value.engine == "mariadb" ? "3306" : "5432"}/${each.value.database}" }] : []),
    secrets = each.value.database != "" ? [
      { name = "DB_USERNAME", valueFrom = "${data.aws_secretsmanager_secret.service[each.key].arn}:username::" },
      { name = "DB_PASSWORD", valueFrom = "${data.aws_secretsmanager_secret.service[each.key].arn}:password::" }
    ] : [],
    logConfiguration = { logDriver = "awslogs", options = { "awslogs-group" = aws_cloudwatch_log_group.service[each.key].name, "awslogs-region" = "ap-northeast-2", "awslogs-stream-prefix" = "ecs" } },
    healthCheck      = { command = ["CMD-SHELL", "wget -q -O- http://localhost:${each.value.port}/actuator/health/liveness || exit 1"], interval = 30, timeout = 5, retries = 3, startPeriod = 90 }
  }])
  tags = { zone = each.value.zone }
  lifecycle {
    precondition {
      condition     = var.enable_endpoints
      error_message = "Private Fargate requires VPC endpoints; do not add NAT outside FEP."
    }
    precondition {
      condition     = contains(keys(var.images), each.key)
      error_message = "Every enabled service needs an explicit immutable image URI."
    }
    precondition {
      condition     = each.value.database == "" || contains(keys(var.secret_names), each.key)
      error_message = "Reference an existing service secret NAME. Never provide a password in Terraform."
    }
  }
}
resource "aws_ecs_service" "service" {
  for_each        = local.active
  name            = each.key
  cluster         = aws_ecs_cluster.zone[each.value.zone].id
  task_definition = aws_ecs_task_definition.service[each.key].arn
  desired_count   = 1
  launch_type     = "FARGATE"
  network_configuration {
    assign_public_ip = false
    subnets          = var.zones[each.value.zone].app_subnets
    security_groups  = [var.app_sgs[each.value.group]]
  }
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }
  dynamic "load_balancer" {
    for_each = each.key == "api-gateway" && var.enable_public_ingress ? [1] : []
    content {
      target_group_arn = aws_lb_target_group.gateway[0].arn
      container_name   = "api-gateway"
      container_port   = 8080
    }
  }
  tags       = { zone = each.value.zone }
  depends_on = [aws_lb_listener.https]
}
resource "aws_security_group" "alb" {
  count       = var.enable_public_ingress && var.enabled ? 1 : 0
  name_prefix = "${var.prefix}-alb-"
  vpc_id      = var.zones.main.vpc_id
  tags        = { zone = "main" }
}
resource "aws_vpc_security_group_ingress_rule" "https" {
  count             = var.enable_public_ingress && var.enabled ? 1 : 0
  security_group_id = aws_security_group.alb[0].id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}
resource "aws_vpc_security_group_egress_rule" "alb" {
  count                        = var.enable_public_ingress && var.enabled ? 1 : 0
  security_group_id            = aws_security_group.alb[0].id
  referenced_security_group_id = var.app_sgs.main_channel
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080
}
resource "aws_vpc_security_group_ingress_rule" "gateway" {
  count                        = var.enable_public_ingress && var.enabled ? 1 : 0
  security_group_id            = var.app_sgs.main_channel
  referenced_security_group_id = aws_security_group.alb[0].id
  ip_protocol                  = "tcp"
  from_port                    = 8080
  to_port                      = 8080
}
resource "aws_lb" "gateway" {
  count              = var.enable_public_ingress && var.enabled ? 1 : 0
  name               = "${var.prefix}-gateway"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb[0].id]
  subnets            = var.public_subnets
  tags               = { zone = "main" }
}
resource "aws_lb_target_group" "gateway" {
  count       = var.enable_public_ingress && var.enabled ? 1 : 0
  name        = "${var.prefix}-gateway"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = var.zones.main.vpc_id
  health_check { path = "/actuator/health/liveness" }
  tags = { zone = "main" }
}
resource "aws_lb_listener" "https" {
  count             = var.enable_public_ingress && var.enabled ? 1 : 0
  load_balancer_arn = aws_lb.gateway[0].arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = var.certificate_arn
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.gateway[0].arn
  }
  tags = { zone = "main" }
}
