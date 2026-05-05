output "vpc_id" {
  value = aws_vpc.this.id
}

output "public_subnet_ids" {
  value = [for subnet in aws_subnet.public : subnet.id]
}

output "ecs_private_subnet_ids" {
  value = [for subnet in aws_subnet.ecs_private : subnet.id]
}

output "data_private_subnet_ids" {
  value = [for subnet in aws_subnet.data_private : subnet.id]
}

output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "rds_endpoint" {
  value = aws_db_instance.this.address
}

output "redis_host" {
  value = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "github_actions_aws_role_arn" {
  value = aws_iam_role.github_actions.arn
}

# ==========================================
# Customer
# ==========================================

output "customer_ecr_repository_url" {
  value = aws_ecr_repository.customer.repository_url
}

output "customer_ecr_repository_name" {
  value = aws_ecr_repository.customer.name
}

output "customer_service_name" {
  value = aws_ecs_service.customer.name
}

output "customer_container_name" {
  value = var.customer_container_name
}

output "customer_target_group_arn" {
  value = aws_lb_target_group.customer.arn
}

output "customer_cloudwatch_log_group_name" {
  value = aws_cloudwatch_log_group.customer.name
}


# ==========================================
# Admin
# ==========================================

output "admin_ecr_repository_url" {
  value = aws_ecr_repository.admin.repository_url
}

output "admin_ecr_repository_name" {
  value = aws_ecr_repository.admin.name
}

output "admin_ecs_service_name" {
  value = var.admin_enabled ? aws_ecs_service.admin[0].name : null
}

output "admin_ecs_container_name" {
  value = var.admin_container_name
}

output "admin_instance_ids" {
  value = [for instance in aws_instance.admin : instance.id]
}

output "admin_private_ips" {
  value = [for instance in aws_instance.admin : instance.private_ip]
}

# ==========================================
# Alarm
# ==========================================
output "alarm_ecr_repository_url" {
  value = aws_ecr_repository.alarm.repository_url
}

output "alarm_ecr_repository_name" {
  value = aws_ecr_repository.alarm.name
}

output "alarm_ecs_service_name" {
  value = aws_ecs_service.alarm.name
}

output "alarm_ecs_container_name" {
  value = var.alarm_container_name
}

# ==========================================
# Chat
# ==========================================
output "chat_ecr_repository_url" {
  value = aws_ecr_repository.chat.repository_url
}

output "chat_ecr_repository_name" {
  value = aws_ecr_repository.chat.name
}

output "chat_ecs_service_name" {
  value = aws_ecs_service.chat.name
}

output "chat_ecs_container_name" {
  value = var.chat_container_name
}

# ==========================================
# Monitoring
# ==========================================
output "sns_alert_topic_arn" {
  description = "SNS topic ARN for CloudWatch alarms"
  value       = var.monitoring_enabled ? aws_sns_topic.alerts[0].arn : null
}

output "alert_email" {
  description = "Email address subscribed to SNS alerts"
  value       = var.alert_email
}

output "cloudwatch_dashboard_name" {
  description = "CloudWatch dashboard name"
  value       = var.monitoring_enabled ? aws_cloudwatch_dashboard.main[0].dashboard_name : null
}

output "slack_alert_enabled" {
  description = "Whether Slack alert integration is enabled"
  value       = var.slack_alert_enabled
}