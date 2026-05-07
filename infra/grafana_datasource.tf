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
# Grafana Provider 가 Grafana API 호출할 때 해당 토큰으로 인증
resource "aws_grafana_workspace_service_account_token" "terraform" {
  count = var.managed_grafana_enabled ? 1 : 0

  workspace_id       = aws_grafana_workspace.this[0].id
  service_account_id = aws_grafana_workspace_service_account.terraform[0].service_account_id
  name               = "${local.name_prefix}-${var.environment}-terraform-token"

  # 약 30일
  seconds_to_live = 2592000
}
