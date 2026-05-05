# ECS Task에서 발생하는 Log는 CloudWatch log group으로 전송됨
# log group에 전송된 로그에서 "ERROR" 패턴이 포함된 로그를 필터링하여
# 커스텀 CloudWatch Metric으로 카운팅
resource "aws_cloudwatch_log_metric_filter" "error_logs" {
  for_each = var.monitoring_enabled ? local.log_groups : {}

  name           = "${local.name_prefix}-${var.environment}-${each.key}-error-logs"
  log_group_name = each.value
  pattern        = "ERROR"

  metric_transformation {
    name      = "${each.key}-ErrorLogCount"
    namespace = "${local.name_prefix}/${var.environment}/ApplicationLogs"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "error_logs_high" {
  for_each = var.monitoring_enabled ? local.log_groups : {}

  alarm_name          = "${local.name_prefix}-${var.environment}-${each.key}-error-logs-high"
  alarm_description   = "${each.key} application ERROR logs are too high"
  namespace           = "${local.name_prefix}/${var.environment}/ApplicationLogs"
  metric_name         = "${each.key}-ErrorLogCount"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  depends_on = [
    aws_cloudwatch_log_metric_filter.error_logs
  ]

  tags = local.common_tags
}