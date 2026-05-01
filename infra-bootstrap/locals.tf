locals {
  name_prefix = "${var.project_name}-${var.environment}"

  state_bucket_name = "${local.name_prefix}-state-${data.aws_caller_identity.current.account_id}"
  lock_table_name   = "${local.name_prefix}-lock"

  common_tags = {
    Project     = var.project_name
    Environment = var.environment
    ManagedBy   = "Terraform"
  }
}