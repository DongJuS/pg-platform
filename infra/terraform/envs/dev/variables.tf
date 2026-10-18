variable "project" {
  type    = string
  default = "pg-platform"
}
variable "enable_nat" {
  type    = bool
  default = false
}
variable "enable_rds" {
  type    = bool
  default = false
}
variable "enable_redis" {
  type    = bool
  default = false
}
variable "enable_msk" {
  type    = bool
  default = false
}
variable "enable_compute" {
  type    = bool
  default = false
}
variable "enable_endpoints" {
  type    = bool
  default = false
}
variable "enable_public_ingress" {
  type    = bool
  default = false
}
variable "certificate_arn" {
  type    = string
  default = ""
}
variable "service_images" {
  type    = map(string)
  default = {}
}
variable "service_secret_names" {
  type    = map(string)
  default = {}
}
variable "redis_iam_client_ready" {
  type        = bool
  default     = false
  description = "Set true only after the application has an ElastiCache IAM credentials provider and prefixed keys."
}

variable "enable_ai_nat" {
  type        = bool
  default     = false
  description = "Independent AI VPC egress. This creates code intent only; default remains disabled."
}
variable "ai_llm_egress_cidrs" {
  type        = set(string)
  default     = []
  description = "Explicit external LLM IPv4 destination CIDRs. Empty keeps AI internet access closed."
}
