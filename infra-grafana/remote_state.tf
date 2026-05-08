data "terraform_remote_state" "infra" {
  backend = "s3"

  config = {
    bucket         = var.infra_state_bucket
    key            = var.infra_state_key
    region         = var.aws_region
    dynamodb_table = var.infra_state_lock_table
    encrypt        = true
    kms_key_id     = var.infra_state_kms_key_id
  }
}