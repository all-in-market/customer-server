# ECS 기준으로 AWS Alarm에서 알림을 보낼 조건을 설정하는 파일

resource "aws_cloudwatch_metric_alarm" "ecs_cpu_high" {
  for_each = var.monitoring_enabled ? local.ecs_services : {}

  alarm_name        = "${local.name_prefix}-${var.environment}-${each.key}-ecs-cpu-high"
  alarm_description = "${each.key} ECS CPU utilization is high"
  namespace         = "AWS/ECS"

  # 5분 평균 CPU 사용률이 80% 이상 상태가 2번 연속 발생하면 알림
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data = "notBreaching"

  dimensions = {
    ClusterName = aws_ecs_cluster.this.name
    ServiceName = each.value
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "ecs_memory_high" {
  for_each = var.monitoring_enabled ? local.ecs_services : {}

  alarm_name        = "${local.name_prefix}-${var.environment}-${each.key}-ecs-memory-high"
  alarm_description = "${each.key} ECS memory utilization is high"
  namespace         = "AWS/ECS"

  # 5분 평균 메모리 사용률이 80% 이상 상태가 2번 연속 발생하면 알림
  metric_name         = "MemoryUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data = "notBreaching"

  dimensions = {
    ClusterName = aws_ecs_cluster.this.name
    ServiceName = each.value
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}