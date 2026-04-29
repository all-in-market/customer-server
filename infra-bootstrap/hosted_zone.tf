# Hosted Zone 생성
resource "aws_route53_zone" "public" {
  name = var.route53_zone_name

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-hosted-zone"
  })
}