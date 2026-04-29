resource "aws_kms_key" "ssm" {
  description = "KMS key for SSM SecureString"

  deletion_window_in_days = 30
  enable_key_rotation     = true

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-ssm-kms"
  })
}

resource "aws_kms_alias" "ssm" {
  name          = "alias/${local.name_prefix}-ssm"
  target_key_id = aws_kms_key.ssm.key_id
}

resource "aws_ssm_parameter" "db_password" {
  name  = "/${var.project_name}/${var.environment}/db/password"
  type  = "SecureString"
  value = var.db_password
  key_id = aws_kms_key.ssm.arn

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-db-password"
  })
}