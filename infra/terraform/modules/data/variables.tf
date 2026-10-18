variable "prefix" { type = string }
variable "enabled_rds" { type = bool }
variable "enabled_redis" { type = bool }
variable "enabled_msk" { type = bool }
variable "subnets" { type = list(string) }
variable "security_groups" { type = map(string) }
