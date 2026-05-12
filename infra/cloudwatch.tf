# Buyer-Seller 서버용 로그 관리 그룹 생성
resource "aws_cloudwatch_log_group" "customer" {
  name              = local.customer_log_group_name
  retention_in_days = var.log_retention_in_days

  # S3 access log는 트래픽이 적으로 현재 단계에서는 생략

  tags = merge(local.common_tags, {
    Name = local.customer_log_group_name
  })
}

# Admin 서버용 로그 관리 그룹 생성
resource "aws_cloudwatch_log_group" "admin" {
  name              = local.admin_log_group_name
  retention_in_days = var.log_retention_in_days

  tags = merge(local.common_tags, {
    Name = local.admin_log_group_name
  })
}

# Alarm 서버용 로그 관리 그룹 생성
resource "aws_cloudwatch_log_group" "alarm" {
  name              = local.alarm_log_group_name
  retention_in_days = var.log_retention_in_days

  tags = merge(local.common_tags, {
    Name = local.alarm_log_group_name
  })
}

# Chat 서버용 로그 관리 그룹 생성
resource "aws_cloudwatch_log_group" "chat" {
  name              = local.chat_log_group_name
  retention_in_days = var.log_retention_in_days

  tags = merge(local.common_tags, {
    Name = local.chat_log_group_name
  })
}

# ADOT 컨테이너가 출력하는 로그 저장하는 로그 그룹
resource "aws_cloudwatch_log_group" "adot" {
  count = var.managed_prometheus_enabled ? 1 : 0

  name              = "/ecs/${var.project_name}/adot"
  retention_in_days = var.log_retention_in_days

  tags = merge(local.common_tags, {
    Name = "/ecs/${var.project_name}/adot"
  })
}

# WAF 로그 그룹
resource "aws_cloudwatch_log_group" "waf" {
  count = var.waf_enabled && var.waf_logging_enabled ? 1 : 0

  name              = "aws-waf-logs-${var.project_name}-${var.environment}"
  retention_in_days = var.waf_log_retention_in_days

  tags = merge(local.common_tags, {
    Name = "aws-waf-logs-${var.project_name}-${var.environment}"
  })
}