variable "project" { type = string }
variable "env" { type = string }
variable "enable_nat" { type = bool }
variable "enable_public_ingress" { type = bool }
variable "enable_endpoints" { type = bool }
variable "azs" { type = list(string) }

variable "enable_ai_nat" {
  type    = bool
  default = false
}
