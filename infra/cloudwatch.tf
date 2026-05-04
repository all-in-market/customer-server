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