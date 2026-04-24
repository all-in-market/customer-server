resource "aws_cloudwatch_log_group" "ecs" {
  name              = local.log_group_name
  retention_in_days = var.log_retention_in_days

  # S3 access log는 트래픽이 적으로 현재 단계에서는 생략

  tags = merge(local.common_tags, {
    Name = local.log_group_name
  })
}
