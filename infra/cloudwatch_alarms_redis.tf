# Redis 기준으로 AWS Alarm에서 알림을 보낼 조건을 설정하는 파일

resource "aws_cloudwatch_metric_alarm" "redis_cpu_high" {
  count = var.monitoring_enabled ? 1 : 0

  alarm_name = "${local.name_prefix}-${var.environment}-redis-cpu-high"
  namespace  = "AWS/ElastiCache"

  # 5분 평균 Redis CPU 사용률이 80% 이상 상태가 2번 연속 발생하면 알림
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data = "notBreaching"

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.this.replication_group_id
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "redis_memory_high" {
  count = var.monitoring_enabled ? 1 : 0

  alarm_name = "${local.name_prefix}-${var.environment}-redis-memory-high"
  namespace  = "AWS/ElastiCache"

  # 5분 평균 Redis 메모리 사용률이 80% 이상 상태가 2번 연속 발생하면 알림
  metric_name         = "DatabaseMemoryUsagePercentage"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data = "notBreaching"

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.this.replication_group_id
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "redis_evictions" {
  count = var.monitoring_enabled ? 1 : 0

  alarm_name = "${local.name_prefix}-${var.environment}-redis-evictions"
  namespace  = "AWS/ElastiCache"

  # 5분 평균 Redis Evictions(메모리 부족 시 데이터 강제 삭제)가 1번이라도 발생하면 알림
  metric_name         = "Evictions"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data = "notBreaching"

  dimensions = {
    ReplicationGroupId = aws_elasticache_replication_group.this.replication_group_id
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}