provider "aws" {
  region = var.aws_region
}

provider "grafana" {
  url  = "https://${data.terraform_remote_state.infra.outputs.grafana_workspace_endpoint}"
  auth = data.terraform_remote_state.infra.outputs.grafana_service_account_token
}