variable "prefix" { type = string }
variable "zones" { type = map(object({ vpc_id = string, endpoint_sg = string })) }
locals {
  groups = { main_channel = "main", main_core = "main", main_external = "main", main_info = "main", ai = "ai", fep = "fep", mock = "mock" }
  flows = {
    channel_core  = { from = "main_channel", to = "main_core", start = 8081, end = 8086 },
    channel_info  = { from = "main_channel", to = "main_info", start = 8088, end = 8089 },
    core_core     = { from = "main_core", to = "main_core", start = 8081, end = 8086 },
    core_external = { from = "main_core", to = "main_external", start = 8087, end = 8087 },
    info_core     = { from = "main_info", to = "main_core", start = 8081, end = 8086 },
    info_ai       = { from = "main_info", to = "ai", start = 8090, end = 8090 },
    core_fep      = { from = "main_core", to = "fep", start = 8091, end = 8091 },
    external_fep  = { from = "main_external", to = "fep", start = 8091, end = 8091 },
    info_fep      = { from = "main_info", to = "fep", start = 8091, end = 8091 },
    fep_mock      = { from = "fep", to = "mock", start = 8092, end = 8095 },
    mock_fep      = { from = "mock", to = "fep", start = 8091, end = 8091 }
  }
  data = { postgres = 5432, mariadb = 3306, redis = 6379, kafka = 9098 }
  clients = {
    postgres = ["main_core", "main_external", "main_info", "ai", "fep"],
    mariadb  = ["main_core"],
    redis    = ["main_channel", "main_core", "main_external", "main_info", "ai", "fep"],
    kafka    = ["main_core", "main_external", "main_info", "ai", "fep"]
  }
  connections = merge([for store, clients in local.clients : {
    for client in clients : "${store}-${client}" => { store = store, client = client }
  }]...)
}
resource "aws_security_group" "app" {
  for_each    = local.groups
  name_prefix = "${var.prefix}-${each.key}-"
  vpc_id      = var.zones[each.value].vpc_id
  tags        = { zone = each.value }
}
resource "aws_security_group" "data" {
  for_each    = local.data
  name_prefix = "${var.prefix}-${each.key}-"
  vpc_id      = var.zones.main.vpc_id
  tags        = { zone = "main" }
}
resource "aws_vpc_security_group_ingress_rule" "flow" {
  for_each                     = local.flows
  security_group_id            = aws_security_group.app[each.value.to].id
  referenced_security_group_id = aws_security_group.app[each.value.from].id
  ip_protocol                  = "tcp"
  from_port                    = each.value.start
  to_port                      = each.value.end
}
resource "aws_vpc_security_group_egress_rule" "flow" {
  for_each                     = local.flows
  security_group_id            = aws_security_group.app[each.value.from].id
  referenced_security_group_id = aws_security_group.app[each.value.to].id
  ip_protocol                  = "tcp"
  from_port                    = each.value.start
  to_port                      = each.value.end
}
resource "aws_vpc_security_group_ingress_rule" "data" {
  for_each                     = local.connections
  security_group_id            = aws_security_group.data[each.value.store].id
  referenced_security_group_id = aws_security_group.app[each.value.client].id
  ip_protocol                  = "tcp"
  from_port                    = local.data[each.value.store]
  to_port                      = local.data[each.value.store]
}
resource "aws_vpc_security_group_egress_rule" "data" {
  for_each                     = local.connections
  security_group_id            = aws_security_group.app[each.value.client].id
  referenced_security_group_id = aws_security_group.data[each.value.store].id
  ip_protocol                  = "tcp"
  from_port                    = local.data[each.value.store]
  to_port                      = local.data[each.value.store]
}
resource "aws_vpc_security_group_ingress_rule" "endpoint" {
  for_each                     = local.groups
  security_group_id            = var.zones[each.value].endpoint_sg
  referenced_security_group_id = aws_security_group.app[each.key].id
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
}
resource "aws_vpc_security_group_egress_rule" "endpoint" {
  for_each                     = local.groups
  security_group_id            = aws_security_group.app[each.key].id
  referenced_security_group_id = var.zones[each.value].endpoint_sg
  ip_protocol                  = "tcp"
  from_port                    = 443
  to_port                      = 443
}
# Image layers/PDF use S3 gateway endpoints. Restrict egress to the AWS-managed S3 prefix list.
data "aws_prefix_list" "s3" { name = "com.amazonaws.ap-northeast-2.s3" }
resource "aws_vpc_security_group_egress_rule" "s3" {
  for_each          = local.groups
  security_group_id = aws_security_group.app[each.key].id
  prefix_list_id    = data.aws_prefix_list.s3.id
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}
resource "aws_vpc_security_group_egress_rule" "fep_internet" {
  security_group_id = aws_security_group.app["fep"].id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}
output "app" { value = { for k, v in aws_security_group.app : k => v.id } }
output "data" { value = { for k, v in aws_security_group.data : k => v.id } }

# CIDRs provide the network restriction; exact hostname checks are implemented at the LLM client seam.
variable "ai_llm_egress_cidrs" {
  type    = set(string)
  default = []
  validation {
    condition     = alltrue([for cidr in var.ai_llm_egress_cidrs : can(cidrnetmask(cidr)) && try(cidrnetmask(cidr), "0.0.0.0") != "0.0.0.0"])
    error_message = "Use explicit IPv4 destination CIDRs, never an unrestricted AI internet route."
  }
}
resource "aws_vpc_security_group_egress_rule" "ai_llm" {
  for_each          = var.ai_llm_egress_cidrs
  security_group_id = aws_security_group.app["ai"].id
  cidr_ipv4         = each.value
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}
