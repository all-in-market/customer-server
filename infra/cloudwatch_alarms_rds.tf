# RDS 기준으로 AWS Alarm에서 알림을 보낼 조건을 설정하는 파일

resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  count = var.monitoring_enabled ? 1 : 0

  alarm_name          = "${local.name_prefix}-${var.environment}-rds-cpu-high"
  namespace           = "AWS/RDS"

  # 5분 평균 RDS CPU 사용률이 80% 이상 상태가 2번 연속 발생하면 알림
  metric_name         = "CPUUtilization"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = 80
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.this.identifier
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "rds_free_storage_low" {
  count = var.monitoring_enabled ? 1 : 0

  alarm_name          = "${local.name_prefix}-${var.environment}-rds-free-storage-low"
  namespace           = "AWS/RDS"

  # 5분 평균 RDS 메모리 저장공간이 5GB 미만 상태가 2번 연속 발생하면 알림
  metric_name         = "FreeStorageSpace"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2

  # 5GB 미만
  threshold           = 5368709120
  comparison_operator = "LessThanThreshold"

  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.this.identifier
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}

resource "aws_cloudwatch_metric_alarm" "rds_connections_high" {
  count = var.monitoring_enabled ? 1 : 0

  alarm_name          = "${local.name_prefix}-${var.environment}-rds-connections-high"
  namespace           = "AWS/RDS"

  #5분 평균 DB 커넥션 수가 db_max_connections의 80% 이상인 상태가 2번 연속 발생하면 알람 발생
  metric_name         = "DatabaseConnections"
  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = local.db_connection_threshold
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data  = "notBreaching"

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.this.identifier
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}