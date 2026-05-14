# ALB 앞단에서 요청을 검사하고 차단/허용 규칙을 적용할 제어 규칙 목록(ACL)
resource "aws_wafv2_web_acl" "alb" {
  count = var.waf_enabled ? 1 : 0

  name        = "${local.name_prefix}-${var.environment}-alb-waf"
  description = "WAF for ${local.name_prefix} public ALB"
  scope       = "REGIONAL"

  default_action {
    allow {}
  }

  # 1. AWS가 관리하는 악성 IP 목록 기반 차단
  rule {
    name     = "AWSManagedRulesAmazonIpReputationList"
    priority = 10

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesAmazonIpReputationList"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name_prefix}-${var.environment}-ip-reputation"
      sampled_requests_enabled   = true
    }
  }

  # 2. 일반적인 웹 공격 방어 (XSS, 비정상 요청, 크기 제한 등, /ws-chat 로 시작하는 경로의 요청은 제외)
  # 이미지 전송 사이즈 제한 해제
  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 20

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"

        scope_down_statement {
          not_statement {
            statement {
              byte_match_statement {
                search_string = "/ws-chat"

                field_to_match {
                  uri_path {}
                }

                positional_constraint = "STARTS_WITH"

                text_transformation {
                  priority = 0
                  type     = "NONE"
                }
              }
            }
          }
        }

        rule_action_override {
          name = "SizeRestrictions_BODY"

          action_to_use {
            count {}
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name_prefix}-${var.environment}-common"
      sampled_requests_enabled   = true
    }
  }

  # 3. 악성 입력 패턴 차단 (Command Injection 등등)
  rule {
    name     = "AWSManagedRulesKnownBadInputsRuleSet"
    priority = 30

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name_prefix}-${var.environment}-bad-inputs"
      sampled_requests_enabled   = true
    }
  }

  # 4. SQL Injection 공격 패턴 차단
  rule {
    name     = "AWSManagedRulesSQLiRuleSet"
    priority = 40

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesSQLiRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name_prefix}-${var.environment}-sqli"
      sampled_requests_enabled   = true
    }
  }

  # 5. IP 기준 Rate Limit
  rule {
    name     = "RateLimitPerIp"
    priority = 50

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = var.waf_rate_limit_per_5min
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${local.name_prefix}-${var.environment}-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${local.name_prefix}-${var.environment}-alb-waf"
    sampled_requests_enabled   = true
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-${var.environment}-alb-waf"
  })
}

# 현재 인프라의 ALB에 WAF 연결
resource "aws_wafv2_web_acl_association" "alb" {
  count = var.waf_enabled ? 1 : 0

  resource_arn = aws_lb.this.arn
  web_acl_arn  = aws_wafv2_web_acl.alb[0].arn
}

# WAF가 검사한 요청 로그를 CloudWatch Logs로 보내는 설정
resource "aws_wafv2_web_acl_logging_configuration" "alb" {
  count = var.waf_enabled && var.waf_logging_enabled ? 1 : 0

  resource_arn            = aws_wafv2_web_acl.alb[0].arn
  log_destination_configs = [aws_cloudwatch_log_group.waf[0].arn]

  # Authorization 헤더 로그에서 제거
  redacted_fields {
    single_header {
      name = "authorization"
    }
  }

  # 쿠키 정보 로그에서 제거
  redacted_fields {
    single_header {
      name = "cookie"
    }
  }

  depends_on = [
    aws_wafv2_web_acl.alb,
    aws_cloudwatch_log_group.waf
  ]
}