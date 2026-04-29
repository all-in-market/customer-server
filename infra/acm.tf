# 인증서 생성 요청 (pending 상태)
resource "aws_acm_certificate" "app" {
  domain_name       = var.domain_name

  # ACM 검증용 DNS 레코드 생성됨
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-certificate"
  })
}

data "aws_route53_zone" "public" {
  name         = var.route53_zone_name
  private_zone = false
}

resource "aws_route53_record" "acm_validation" {

  # ACM이 발급한 검증용 DNS 레코드들을 위에서 생성한 Hosted Zone에 저장
  for_each = {
    for dvo in aws_acm_certificate.app.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  zone_id         = data.aws_route53_zone.public.zone_id
  name            = each.value.name
  type            = each.value.type
  ttl             = 60
  records         = [each.value.record]
}

# 맨 위에서 생성 요청한 인증서가 검증 완료될 때까지 기다리는 리소스
resource "aws_acm_certificate_validation" "app" {
  certificate_arn         = aws_acm_certificate.app.arn
  validation_record_fqdns = [for record in aws_route53_record.acm_validation : record.fqdn]
}

# Hosted zone으로 들어온 DNS 질의를 어디로 연결할지 정하는 A 레코드 생성
resource "aws_route53_record" "app_alias" {
  depends_on = [aws_lb_listener.https]

  zone_id = data.aws_route53_zone.public.zone_id
  name    = var.domain_name
  type    = "A"

  # 생성해둔 ALB로 연결
  alias {
    name                   = aws_lb.this.dns_name
    zone_id                = aws_lb.this.zone_id
    evaluate_target_health = false
  }
}
