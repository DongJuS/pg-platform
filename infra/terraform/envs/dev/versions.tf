terraform {
  required_version = "~> 1.16.6"
  required_providers { aws = { source = "hashicorp/aws", version = "~> 6.69.0" } }
  backend "s3" {}
}
provider "aws" {
  region = "ap-northeast-2"
  default_tags { tags = { project = var.project, env = "dev", zone = "shared" } }
}
