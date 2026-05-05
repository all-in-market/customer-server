resource "aws_iam_role" "chatbot_slack" {
  count = var.monitoring_enabled && var.slack_alert_enabled ? 1 : 0

  name = "${local.name_prefix}-${var.environment}-chatbot-slack-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "chatbot.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-${var.environment}-chatbot-slack-role"
  })
}

resource "aws_iam_role_policy_attachment" "chatbot_cloudwatch_readonly" {
  count = var.monitoring_enabled && var.slack_alert_enabled ? 1 : 0

  role       = aws_iam_role.chatbot_slack[0].name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchReadOnlyAccess"
}

resource "aws_chatbot_slack_channel_configuration" "alerts" {
  count = var.monitoring_enabled && var.slack_alert_enabled ? 1 : 0

  configuration_name = "${local.name_prefix}-${var.environment}-alerts"
  iam_role_arn       = aws_iam_role.chatbot_slack[0].arn
  slack_team_id      = var.slack_team_id
  slack_channel_id   = var.slack_channel_id

  # 해당 토픽으로 전송되는 알림이 최종적으로 슬랙에 전달됨
  sns_topic_arns = [
    aws_sns_topic.alerts[0].arn
  ]

  logging_level = "ERROR"

  guardrail_policy_arns = [
    "arn:aws:iam::aws:policy/CloudWatchReadOnlyAccess"
  ]

  user_authorization_required = false

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-${var.environment}-slack-alerts"
  })

  depends_on = [
    aws_iam_role_policy_attachment.chatbot_cloudwatch_readonly
  ]
}