locals {
  alert_actions = var.monitoring_enabled ? [aws_sns_topic.alerts[0].arn] : []

  db_connection_threshold = floor(var.db_max_connections * 0.8)

  ecs_services = merge(
    {
      customer = aws_ecs_service.customer.name
      alarm    = aws_ecs_service.alarm.name
      chat     = aws_ecs_service.chat.name
    },
      var.admin_enabled ? {
      admin = aws_ecs_service.admin[0].name
    } : {}
  )

  target_groups = merge(
    {
      customer = aws_lb_target_group.customer.arn_suffix
      alarm    = aws_lb_target_group.alarm.arn_suffix
      chat     = aws_lb_target_group.chat.arn_suffix
    },
      var.admin_enabled ? {
      admin = aws_lb_target_group.admin[0].arn_suffix
    } : {}
  )

  log_groups = merge(
    {
      customer = aws_cloudwatch_log_group.customer.name
      alarm    = aws_cloudwatch_log_group.alarm.name
      chat     = aws_cloudwatch_log_group.chat.name
    },
      var.admin_enabled ? {
      admin = aws_cloudwatch_log_group.admin.name
    } : {}
  )
}