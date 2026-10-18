mock_provider "aws" {
  mock_data "aws_prefix_list" { defaults = { id = "pl-0123456789abcdef0" } }
}
variables {
  prefix = "pg-platform-test"
  zones = {
    main = { vpc_id = "vpc-00000000000000001", endpoint_sg = "sg-00000000000000001" }
    ai   = { vpc_id = "vpc-00000000000000002", endpoint_sg = "sg-00000000000000002" }
    fep  = { vpc_id = "vpc-00000000000000003", endpoint_sg = "sg-00000000000000003" }
    mock = { vpc_id = "vpc-00000000000000004", endpoint_sg = "sg-00000000000000004" }
  }
}
run "fep_rest_is_internal_and_ai_destinations_start_closed" {
  command = plan
  assert {
    condition     = alltrue([for flow in ["core_fep", "external_fep", "info_fep"] : aws_vpc_security_group_ingress_rule.flow[flow].from_port == 8091 && aws_vpc_security_group_egress_rule.flow[flow].to_port == 8091])
    error_message = "Caller groups must reach internal FEP REST on port 8091."
  }
  assert {
    condition     = length(aws_vpc_security_group_egress_rule.ai_llm) == 0
    error_message = "AI internet destinations must be closed until explicitly supplied."
  }
}
run "explicit_ai_destinations_are_https_only" {
  command = plan
  variables { ai_llm_egress_cidrs = ["203.0.113.0/24"] }
  assert {
    condition     = length(aws_vpc_security_group_egress_rule.ai_llm) == 1 && aws_vpc_security_group_egress_rule.ai_llm["203.0.113.0/24"].from_port == 443 && aws_vpc_security_group_egress_rule.ai_llm["203.0.113.0/24"].to_port == 443
    error_message = "Explicit destination rules must only allow HTTPS."
  }
}

run "rejects_disguised_unrestricted_ai_destination" {
  command = plan
  variables { ai_llm_egress_cidrs = ["0.0.0.1/0"] }
  expect_failures = [var.ai_llm_egress_cidrs]
}
