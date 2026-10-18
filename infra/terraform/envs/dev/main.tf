locals {
  prefix = "${var.project}-dev"
  services = {
    "api-gateway"   = { zone = "main", group = "main_channel", port = 8080, database = "", engine = "" }
    "auth"          = { zone = "main", group = "main_core", port = 8081, database = "auth_db", engine = "postgresql" }
    "account"       = { zone = "main", group = "main_core", port = 8082, database = "account_db", engine = "mariadb" }
    "ledger"        = { zone = "main", group = "main_core", port = 8083, database = "ledger_db", engine = "mariadb" }
    "transfer"      = { zone = "main", group = "main_core", port = 8084, database = "transfer_db", engine = "postgresql" }
    "payment"       = { zone = "main", group = "main_core", port = 8085, database = "payment_db", engine = "postgresql" }
    "settlement"    = { zone = "main", group = "main_core", port = 8086, database = "settlement_db", engine = "postgresql" }
    "swift"         = { zone = "main", group = "main_external", port = 8087, database = "swift_db", engine = "postgresql" }
    "wealth-report" = { zone = "main", group = "main_info", port = 8088, database = "wealth_report_db", engine = "postgresql" }
    "aml"           = { zone = "main", group = "main_info", port = 8089, database = "aml_db", engine = "postgresql" }
    "ai-gateway"    = { zone = "ai", group = "ai", port = 8090, database = "ai_gateway_db", engine = "postgresql" }
    "fep-adapter"   = { zone = "fep", group = "fep", port = 8091, database = "fep_adapter_db", engine = "postgresql" }
    "mock-pg"       = { zone = "mock", group = "mock", port = 8092, database = "", engine = "" }
    "mock-swift"    = { zone = "mock", group = "mock", port = 8093, database = "", engine = "" }
    "mock-kofiu"    = { zone = "mock", group = "mock", port = 8094, database = "", engine = "" }
    "mock-clearing" = { zone = "mock", group = "mock", port = 8095, database = "", engine = "" }
  }
}
data "aws_caller_identity" "current" {}
module "network" {
  source                = "../../modules/network"
  project               = var.project
  env                   = "dev"
  enable_nat            = var.enable_nat
  enable_ai_nat         = var.enable_ai_nat
  enable_public_ingress = var.enable_public_ingress
  enable_endpoints      = var.enable_endpoints
  azs                   = ["ap-northeast-2a", "ap-northeast-2c"]
}
module "peering" {
  source = "../../modules/peering"
  zones  = module.network.zones
}
module "security" {
  source              = "../../modules/security"
  prefix              = local.prefix
  zones               = module.network.zones
  ai_llm_egress_cidrs = var.ai_llm_egress_cidrs
  depends_on          = [module.peering]
}
module "data" {
  source          = "../../modules/data"
  prefix          = local.prefix
  enabled_rds     = var.enable_rds
  enabled_redis   = var.enable_redis
  enabled_msk     = var.enable_msk
  subnets         = module.network.zones.main.data_subnets
  security_groups = module.security.data
}
module "storage" {
  source              = "../../modules/storage"
  prefix              = local.prefix
  account_id          = data.aws_caller_identity.current.account_id
  enable_repositories = var.enable_compute
  services            = toset(keys(local.services))
}
module "compute" {
  source                = "../../modules/compute"
  prefix                = local.prefix
  enabled               = var.enable_compute
  enable_public_ingress = var.enable_public_ingress
  enable_endpoints      = var.enable_endpoints
  certificate_arn       = var.certificate_arn
  services              = local.services
  zones                 = module.network.zones
  app_sgs               = module.security.app
  public_subnets        = module.network.main_public_subnets
  images                = var.service_images
  secret_names          = var.service_secret_names
  rds_endpoints         = { postgresql = lookup(module.data.rds_endpoints, "postgres", ""), mariadb = lookup(module.data.rds_endpoints, "mariadb", "") }
  redis_endpoint        = module.data.redis_endpoint
  redis_user            = module.data.redis_user
  redis_iam_resources   = module.data.redis_iam_resources
  kafka_bootstrap       = module.data.kafka_bootstrap
  kafka_arn             = module.data.kafka_arn
  report_bucket_arn     = module.storage.report_bucket_arn
}
# No resources are deployed by this repository. These preconditions also prevent incomplete opt-in deployments.
resource "terraform_data" "deployment_contract" {
  input = var.enable_compute
  lifecycle {
    precondition {
      condition     = !var.enable_compute || (var.enable_rds && var.enable_redis && var.enable_msk && var.enable_endpoints)
      error_message = "Private compute requires its stores and endpoints; enable all explicit dependencies."
    }
    precondition {
      condition     = !var.enable_compute || var.redis_iam_client_ready
      error_message = "ElastiCache IAM token provider is pending. Do not deploy application tasks until implemented."
    }
    precondition {
      condition     = !var.enable_public_ingress || (var.enable_compute && var.certificate_arn != "")
      error_message = "Public ingress requires compute and an existing ACM certificate ARN."
    }
  }
}
output "database_endpoints" { value = module.data.rds_endpoints }
output "report_bucket" { value = module.storage.report_bucket_arn }
output "repositories" { value = module.storage.repositories }
