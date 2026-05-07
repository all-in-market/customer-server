provider "aws" {
  region = var.aws_region
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

provider "grafana" {
  url  = var.managed_grafana_enabled ? "https://${one(aws_grafana_workspace.this).endpoint}" : ""
  auth = var.managed_grafana_enabled ? one(aws_grafana_workspace_service_account_token.terraform).key : ""
}
