output "backend_bucket" {
  value = aws_s3_bucket.terraform_state.bucket
}

output "backend_dynamodb_table" {
  value = aws_dynamodb_table.terraform_lock.name
}

output "backend_kms_key_id" {
  value = aws_kms_key.terraform_state.arn
}

output "route53_name_servers" {
  value = aws_route53_zone.public.name_servers
}