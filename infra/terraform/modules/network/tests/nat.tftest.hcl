mock_provider "aws" {}
variables {
  project               = "pg-platform-test"
  env                   = "test"
  azs                   = ["ap-northeast-2a", "ap-northeast-2c"]
  enable_public_ingress = false
  enable_endpoints      = false
  enable_nat            = false
  enable_ai_nat         = false
}
run "default_has_no_paid_nat" {
  command = plan
  assert {
    condition     = length(aws_nat_gateway.ai) == 0 && length(aws_nat_gateway.fep) == 0
    error_message = "NAT resources must stay off by default."
  }
  assert {
    condition     = length(aws_route.ai_egress) == 0 && length(aws_route.fep_egress) == 0
    error_message = "Disabled NAT must not leave default egress routes."
  }
}
run "independent_ai_and_fep_app_egress" {
  command = plan
  variables {
    enable_nat    = true
    enable_ai_nat = true
  }
  assert {
    condition     = length(aws_nat_gateway.ai) == 1 && length(aws_nat_gateway.fep) == 1
    error_message = "AI and FEP must own separate NAT gateways."
  }
  assert {
    condition     = toset(keys(aws_route.ai_egress)) == toset(["ai-app-0", "ai-app-1"]) && toset(keys(aws_route.fep_egress)) == toset(["fep-app-0", "fep-app-1"])
    error_message = "Only each VPC's app subnets may get an internet default route."
  }
}
run "ai_egress_does_not_require_fep_nat" {
  command = plan
  variables { enable_ai_nat = true }
  assert {
    condition     = length(aws_nat_gateway.ai) == 1 && length(aws_nat_gateway.fep) == 0
    error_message = "AI NAT must be independent of the FEP NAT toggle."
  }
}
