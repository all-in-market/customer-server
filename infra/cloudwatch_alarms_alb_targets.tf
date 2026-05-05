# ALB, Target 기준으로 AWS Alarm에서 알림을 보낼 조건을 설정하는 파일

# ALB 자체에서 발생한 5xx 에러(로드밸런서 레벨 오류)가 일정 기준 이상이면 알림 발생
resource "aws_cloudwatch_metric_alarm" "alb_5xx" {
  count = var.monitoring_enabled ? 1 : 0

  alarm_name        = "${local.name_prefix}-${var.environment}-alb-5xx"
  alarm_description = "ALB 5XX errors are too high"
  namespace         = "AWS/ApplicationELB"

  # "300"초 단위의 구간 "1"번에서
  # "HTTPCode_ELB_5XX_Count" 값의 "합계"가
  # "5"번 "이상" 발생하면 알림 발생
  metric_name         = "HTTPCode_ELB_5XX_Count"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"

  # 데이터가 없을 시 : 정상으로 간주
  treat_missing_data = "notBreaching"

  # 해당 리소스의 metric만 보겠다는 필터링 조건
  dimensions = {
    LoadBalancer = aws_lb.this.arn_suffix
  }

  # 이벤트 발생(Alarm 상태가 ALARM/OK 로 변경) 시
  # 해당 alert_actions에 등록된 topic으로 메시지 전송
  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}

# Target(ECS/EC2 애플리케이션)에서 발생한 5xx 에러가 일정 기준 이상이면 알림 발생
resource "aws_cloudwatch_metric_alarm" "target_5xx" {
  for_each = var.monitoring_enabled ? local.target_groups : {}

  alarm_name        = "${local.name_prefix}-${var.environment}-${each.key}-target-5xx"
  alarm_description = "${each.key} target 5XX errors are too high"
  namespace         = "AWS/ApplicationELB"

  metric_name         = "HTTPCode_Target_5XX_Count"
  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data = "notBreaching"

  dimensions = {
    LoadBalancer = aws_lb.this.arn_suffix
    TargetGroup  = each.value
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}

# Target 응답시간(p95 기준)이 임계값을 초과하면 알림 발생
resource "aws_cloudwatch_metric_alarm" "target_response_time" {
  for_each = var.monitoring_enabled ? local.target_groups : {}

  alarm_name        = "${local.name_prefix}-${var.environment}-${each.key}-high-latency"
  alarm_description = "${each.key} target response time is too high"
  namespace         = "AWS/ApplicationELB"

  # 5분 평균 p95 값이 2s 초과 상태가 2번 연속 발생하면 알림
  metric_name         = "TargetResponseTime"
  extended_statistic  = "p95"
  period              = 300
  evaluation_periods  = 2
  threshold           = 2
  comparison_operator = "GreaterThanThreshold"

  treat_missing_data = "notBreaching"

  dimensions = {
    LoadBalancer = aws_lb.this.arn_suffix
    TargetGroup  = each.value
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}

# Target Group 내 Unhealthy 상태의 타겟이 존재하면 알림 발생
resource "aws_cloudwatch_metric_alarm" "unhealthy_targets" {
  for_each = var.monitoring_enabled ? local.target_groups : {}

  alarm_name        = "${local.name_prefix}-${var.environment}-${each.key}-unhealthy-target"
  alarm_description = "${each.key} has unhealthy targets"
  namespace         = "AWS/ApplicationELB"

  metric_name         = "UnHealthyHostCount"
  statistic           = "Average"
  period              = 60
  evaluation_periods  = 2
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"

  treat_missing_data = "notBreaching"

  dimensions = {
    LoadBalancer = aws_lb.this.arn_suffix
    TargetGroup  = each.value
  }

  alarm_actions = local.alert_actions
  ok_actions    = local.alert_actions

  tags = local.common_tags
}