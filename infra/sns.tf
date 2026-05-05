# AWS Alarm에서 이벤트 발생 시 메시지가 전송될 Topic 생성
resource "aws_sns_topic" "alerts" {
  count = var.monitoring_enabled ? 1 : 0

  name              = "${local.name_prefix}-${var.environment}-alerts"
  kms_master_key_id = "alias/aws/sns"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-${var.environment}-alerts"
  })
}

# Topic에 구독자를 등록하는 resource
resource "aws_sns_topic_subscription" "email" {
  count = var.monitoring_enabled && var.alert_email != "" ? 1 : 0

  topic_arn = aws_sns_topic.alerts[0].arn
  protocol  = "email"
  endpoint  = var.alert_email
}