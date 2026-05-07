provider "aws" {
  region = var.aws_region
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

provider "grafana" {
  url  = "https://${aws_grafana_workspace.this[0].endpoint}"
  auth = aws_grafana_workspace_service_account_token.terraform[0].key
}
