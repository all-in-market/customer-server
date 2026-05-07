data "aws_ssoadmin_instances" "this" {
  count = var.managed_grafana_enabled ? 1 : 0
}
locals {
  identity_store_id = var.managed_grafana_enabled ? tolist(data.aws_ssoadmin_instances.this[0].identity_store_ids)[0] : ""
}

# IAM Identity Center 로그인에 사용할 사용자 생성
resource "aws_identitystore_user" "grafana_admin" {
  count = var.managed_grafana_enabled ? 1 : 0

  identity_store_id = local.identity_store_id
  user_name         = var.grafana_admin_email
  display_name      = "${var.grafana_admin_given_name} ${var.grafana_admin_family_name}"

  name {
    given_name  = var.grafana_admin_given_name
    family_name = var.grafana_admin_family_name
  }

  emails {
    value   = var.grafana_admin_email
    primary = true
  }
}

# 생성한 IAM Identity Center 사용자를 해당 Grafana Workspace의 ADMIN 권한 사용자로 등록
resource "aws_grafana_role_association" "admin" {
  count = var.managed_grafana_enabled ? 1 : 0

  workspace_id = aws_grafana_workspace.this[0].id
  role         = "ADMIN"

  user_ids = [
    aws_identitystore_user.grafana_admin[0].user_id
  ]

  depends_on = [
    aws_grafana_workspace.this,
    aws_identitystore_user.grafana_admin
  ]
}