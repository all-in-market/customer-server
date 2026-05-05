variable "alert_email" {
  description = "CloudWatch Alarm notification email"
  type        = string
  default     = ""
}

variable "monitoring_enabled" {
  description = "Whether to create CloudWatch alarms and SNS alerts"
  type        = bool
  default     = true
}

variable "slack_alert_enabled" {
  description = "Whether to send CloudWatch alarm notifications to Slack via AWS Chatbot"
  type        = bool
  default     = false
}

variable "slack_channel_id" {
  description = "Slack channel ID for AWS Chatbot"
  type        = string
  default     = ""
}

variable "slack_team_id" {
  description = "Slack workspace(team) ID"
  type        = string
  default     = ""
}