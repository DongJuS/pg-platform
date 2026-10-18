variable "zones" { type = map(object({ vpc_id = string, cidr = string, route_tables = list(string) })) }
locals {
  pairs = { main_ai = ["main", "ai"], main_fep = ["main", "fep"], fep_mock = ["fep", "mock"] }
  routes = merge([for pair, zones in local.pairs : merge([for side in [0, 1] : {
    for index, table in var.zones[zones[side]].route_tables : "${pair}-${side}-${index}" => {
      pair = pair, table = table, cidr = var.zones[zones[1 - side]].cidr
    }
  }]...)]...)
}
resource "aws_vpc_peering_connection" "link" {
  for_each    = local.pairs
  vpc_id      = var.zones[each.value[0]].vpc_id
  peer_vpc_id = var.zones[each.value[1]].vpc_id
  auto_accept = true
  tags        = { Name = each.key, zone = each.key }
  requester { allow_remote_vpc_dns_resolution = true }
  accepter { allow_remote_vpc_dns_resolution = true }
}
resource "aws_route" "peer" {
  for_each                  = local.routes
  route_table_id            = each.value.table
  destination_cidr_block    = each.value.cidr
  vpc_peering_connection_id = aws_vpc_peering_connection.link[each.value.pair].id
}
