output "terraform_state_bucket_name" {
  value = aws_s3_bucket.terraform_state.bucket
}

output "terraform_lock_table_name" {
  value = aws_dynamodb_table.terraform_lock.name
}

output "terraform_state_kms_key_arn" {
  value = aws_kms_key.terraform_state.arn
}