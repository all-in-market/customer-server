# Terraform이 Grafana 내부 리소스를 생성할 때 사용할 Service Account
resource "aws_grafana_workspace_service_account" "terraform" {
  count = var.managed_grafana_enabled ? 1 : 0

  workspace_id = aws_grafana_workspace.this[0].id
  name         = "${local.name_prefix}-${var.environment}-terraform-sa"
  grafana_role = "ADMIN"

  depends_on = [
    aws_grafana_role_association.admin
  ]
}

# Grafana Provider 인증용 Service Account Token
resource "aws_grafana_workspace_service_account_token" "terraform" {
  count = var.managed_grafana_enabled ? 1 : 0

  workspace_id       = aws_grafana_workspace.this[0].id
  service_account_id = aws_grafana_workspace_service_account.terraform[0].service_account_id
  name               = "${local.name_prefix}-${var.environment}-terraform-token"

  # 약 30일
  seconds_to_live = 2592000
}

# Grafana 내부에 Amazon Managed Prometheus datasource 생성
resource "grafana_data_source" "amp" {
  count = var.managed_grafana_enabled && var.managed_prometheus_enabled ? 1 : 0

  type       = "prometheus"
  name       = "${local.name_prefix}-${var.environment}-amp"
  url        = trimsuffix(aws_prometheus_workspace.app[0].prometheus_endpoint, "/")
  is_default = true

  json_data_encoded = jsonencode({
    httpMethod = "POST"

    # Amazon Managed Prometheus 조회를 위한 SigV4 인증
    sigV4Auth     = true
    sigV4AuthType = "default"
    sigV4Region   = var.aws_region
  })

  depends_on = [
    aws_grafana_workspace_service_account_token.terraform,
    aws_iam_role_policy_attachment.grafana_prometheus
  ]
}