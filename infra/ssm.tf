data "aws_iam_policy_document" "ssm_kms_key" {
  statement {
    sid = "EnableRootPermissions"
    actions   = ["kms:*"]
    resources = ["*"]
    principals {
      type        = "AWS"
      identifiers = ["arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"]
    }
  }

  statement {
    sid = "AllowEcsExecutionRoleDecryptForSsm"
    actions   = ["kms:Decrypt", "kms:DescribeKey"]
    resources = ["*"]
    principals {
      type        = "AWS"
      identifiers = [aws_iam_role.ecs_task_execution.arn]
    }
    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${data.aws_region.current.name}.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "kms:EncryptionContext:PARAMETER_ARN"
      values   = ["arn:aws:ssm:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:parameter/${var.project_name}/${var.environment}/db/password"]
    }
  }
}

resource "aws_kms_key" "ssm" {
  description = "KMS key for SSM SecureString"
  policy      = data.aws_iam_policy_document.ssm_kms_key.json

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