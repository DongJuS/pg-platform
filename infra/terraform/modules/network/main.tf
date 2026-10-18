locals {
  zones = { main = "10.10.0.0/16", ai = "10.20.0.0/16", fep = "10.30.0.0/16", mock = "10.40.0.0/16" }
  subnets = merge([
    for zone, cidr in local.zones : merge([
      for tier in ["app", "data"] : {
        for i, az in var.azs : "${zone}-${tier}-${i}" => {
          zone = zone, tier = tier, az = az, index = i,
          cidr = cidrsubnet(cidr, 8, (tier == "app" ? 0 : 10) + i)
        }
      }
    ]...)
  ]...)
  public_zones = merge(var.enable_nat ? { fep = local.zones.fep } : {}, var.enable_ai_nat ? { ai = local.zones.ai } : {}, var.enable_public_ingress ? { main = local.zones.main } : {})
  public_subnets = merge([for zone, cidr in local.public_zones : {
    for i, az in var.azs : "${zone}-public-${i}" => { zone = zone, az = az, index = i, cidr = cidrsubnet(cidr, 8, 20 + i) }
  }]...)
}
resource "aws_vpc" "zone" {
  for_each             = local.zones
  cidr_block           = each.value
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags                 = { Name = "${var.project}-${var.env}-${each.key}", zone = each.key }
}
resource "aws_subnet" "private" {
  for_each                = local.subnets
  vpc_id                  = aws_vpc.zone[each.value.zone].id
  cidr_block              = each.value.cidr
  availability_zone       = each.value.az
  map_public_ip_on_launch = false
  tags                    = { Name = "${var.project}-${var.env}-${each.key}", zone = each.value.zone, tier = each.value.tier }
}
resource "aws_route_table" "private" {
  for_each = local.subnets
  vpc_id   = aws_vpc.zone[each.value.zone].id
  tags     = { Name = "${var.project}-${var.env}-${each.key}", zone = each.value.zone, tier = each.value.tier }
}
resource "aws_route_table_association" "private" {
  for_each       = local.subnets
  subnet_id      = aws_subnet.private[each.key].id
  route_table_id = aws_route_table.private[each.key].id
}
resource "aws_internet_gateway" "public" {
  for_each = local.public_zones
  vpc_id   = aws_vpc.zone[each.key].id
  tags     = { Name = "${var.project}-${var.env}-${each.key}", zone = each.key }
}
resource "aws_subnet" "public" {
  for_each                = local.public_subnets
  vpc_id                  = aws_vpc.zone[each.value.zone].id
  cidr_block              = each.value.cidr
  availability_zone       = each.value.az
  map_public_ip_on_launch = false
  tags                    = { Name = "${var.project}-${var.env}-${each.key}", zone = each.value.zone, tier = "public" }
}
resource "aws_route_table" "public" {
  for_each = local.public_zones
  vpc_id   = aws_vpc.zone[each.key].id
  tags     = { zone = each.key }
}
resource "aws_route" "public_internet" {
  for_each               = local.public_zones
  route_table_id         = aws_route_table.public[each.key].id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.public[each.key].id
}
resource "aws_route_table_association" "public" {
  for_each       = local.public_subnets
  subnet_id      = aws_subnet.public[each.key].id
  route_table_id = aws_route_table.public[each.value.zone].id
}
resource "aws_eip" "nat" {
  count  = var.enable_nat ? 1 : 0
  domain = "vpc"
  tags   = { Name = "${var.project}-${var.env}-fep-nat", zone = "fep" }
}
resource "aws_nat_gateway" "fep" {
  count         = var.enable_nat ? 1 : 0
  allocation_id = aws_eip.nat[0].id
  subnet_id     = aws_subnet.public["fep-public-0"].id
  tags          = { zone = "fep" }
  depends_on    = [aws_internet_gateway.public]
}
resource "aws_route" "fep_egress" {
  for_each               = var.enable_nat ? { for k, v in local.subnets : k => v if v.zone == "fep" && v.tier == "app" } : {}
  route_table_id         = aws_route_table.private[each.key].id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.fep[0].id
}
resource "aws_eip" "ai_nat" {
  count  = var.enable_ai_nat ? 1 : 0
  domain = "vpc"
  tags   = { Name = "${var.project}-${var.env}-ai-nat", zone = "ai" }
}
resource "aws_nat_gateway" "ai" {
  count         = var.enable_ai_nat ? 1 : 0
  allocation_id = aws_eip.ai_nat[0].id
  subnet_id     = aws_subnet.public["ai-public-0"].id
  tags          = { zone = "ai" }
  depends_on    = [aws_internet_gateway.public]
}
resource "aws_route" "ai_egress" {
  for_each               = var.enable_ai_nat ? { for k, v in local.subnets : k => v if v.zone == "ai" && v.tier == "app" } : {}
  route_table_id         = aws_route_table.private[each.key].id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id         = aws_nat_gateway.ai[0].id
}

resource "aws_security_group" "endpoint" {
  for_each    = local.zones
  name_prefix = "${var.project}-${each.key}-endpoint-"
  vpc_id      = aws_vpc.zone[each.key].id
  tags        = { zone = each.key }
}
locals {
  endpoints = var.enable_endpoints ? merge([for zone in keys(local.zones) : {
    for service in ["ecr.api", "ecr.dkr", "logs", "secretsmanager"] : "${zone}-${service}" => { zone = zone, service = service }
  }]...) : {}
}
resource "aws_vpc_endpoint" "interface" {
  for_each            = local.endpoints
  vpc_id              = aws_vpc.zone[each.value.zone].id
  service_name        = "com.amazonaws.ap-northeast-2.${each.value.service}"
  vpc_endpoint_type   = "Interface"
  private_dns_enabled = true
  security_group_ids  = [aws_security_group.endpoint[each.value.zone].id]
  subnet_ids          = [for k, v in local.subnets : aws_subnet.private[k].id if v.zone == each.value.zone && v.tier == "app"]
  tags                = { zone = each.value.zone }
}
resource "aws_vpc_endpoint" "s3" {
  for_each          = local.zones
  vpc_id            = aws_vpc.zone[each.key].id
  service_name      = "com.amazonaws.ap-northeast-2.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = [for k, v in local.subnets : aws_route_table.private[k].id if v.zone == each.key && v.tier == "app"]
  tags              = { zone = each.key }
}
output "zones" {
  value = { for zone, cidr in local.zones : zone => {
    vpc_id       = aws_vpc.zone[zone].id, cidr = cidr,
    app_subnets  = [for k, v in local.subnets : aws_subnet.private[k].id if v.zone == zone && v.tier == "app"],
    data_subnets = [for k, v in local.subnets : aws_subnet.private[k].id if v.zone == zone && v.tier == "data"],
    route_tables = [for k, v in local.subnets : aws_route_table.private[k].id if v.zone == zone],
    endpoint_sg  = aws_security_group.endpoint[zone].id
  } }
}
output "main_public_subnets" { value = [for k, v in local.public_subnets : aws_subnet.public[k].id if v.zone == "main"] }
