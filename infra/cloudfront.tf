#
# # 커스텀 CDN 도메인명을 사용할지, 기본 CDN 도메인명을 사용할지 결정
# locals {
#   product_image_cdn_custom_domain_enabled = length(trimspace(var.product_image_cdn_domain)) > 0
# }
#
# # CDN에 붙일 "정적 파일 최적화 캐시 정책" 조회
# data "aws_cloudfront_cache_policy" "caching_optimized" {
#   name = "Managed-CachingOptimized"
# }
#
# # CloudFront가 SigV4 서명을 사용하여 private S3 버킷에 안전하게 접근할 수 있도록 하는 OAC 설정
# resource "aws_cloudfront_origin_access_control" "product_images" {
#   name                              = "${local.name_prefix}-product-images-oac"
#   description                       = "OAC for product image S3 bucket"
#   origin_access_control_origin_type = "s3"
#   signing_behavior                  = "always"
#   signing_protocol                  = "sigv4"
# }
#
# # cdn.hyu1335.cloud 같은 커스텀 CDN 도메인에
# # HTTPS 적용하기 위한 ACM 인증서 생성
# resource "aws_acm_certificate" "product_image_cdn" {
#   count = local.product_image_cdn_custom_domain_enabled ? 1 : 0
#
#   provider          = aws.us_east_1
#   domain_name       = var.product_image_cdn_domain
#   validation_method = "DNS"
#
#   lifecycle {
#     create_before_destroy = true
#   }
#
#   tags = merge(local.common_tags, {
#     Name = "${local.name_prefix}-product-image-cdn-certificate"
#   })
# }
#
# # ACM이 발급한 검증용 DNS 레코드들을 위에서 생성한 Hosted Zone에 저장
# resource "aws_route53_record" "product_image_cdn_acm_validation" {
#   for_each = local.product_image_cdn_custom_domain_enabled ? {
#     for dvo in aws_acm_certificate.product_image_cdn[0].domain_validation_options : dvo.domain_name => {
#       name   = dvo.resource_record_name
#       record = dvo.resource_record_value
#       type   = dvo.resource_record_type
#     }
#   } : {}
#
#   allow_overwrite = true
#   zone_id         = data.aws_route53_zone.public.zone_id
#   name            = each.value.name
#   type            = each.value.type
#   ttl             = 60
#   records         = [each.value.record]
# }
#
# # ACM 인증서의 DNS 검증을 실제로 완료시키는 리소스
# resource "aws_acm_certificate_validation" "product_image_cdn" {
#   count = local.product_image_cdn_custom_domain_enabled ? 1 : 0
#
#   provider                = aws.us_east_1
#   certificate_arn         = aws_acm_certificate.product_image_cdn[0].arn
#   validation_record_fqdns = [for record in aws_route53_record.product_image_cdn_acm_validation : record.fqdn]
# }
#
# # CDN 생성
# resource "aws_cloudfront_distribution" "product_images" {
#   enabled         = true
#   is_ipv6_enabled = true
#   comment         = "${local.name_prefix} product image CDN"
#
#   aliases = local.product_image_cdn_custom_domain_enabled ? [var.product_image_cdn_domain] : []
#
#   # 이미지를 가져올 원본 서버를 S3 버킷으로 지정
#   origin {
#     domain_name              = aws_s3_bucket.product_images.bucket_regional_domain_name
#     origin_id                = "product-images-s3-origin"
#     origin_access_control_id = aws_cloudfront_origin_access_control.product_images.id
#   }
#
#   default_cache_behavior {
#     target_origin_id       = "product-images-s3-origin"
#     viewer_protocol_policy = "redirect-to-https"
#
#     allowed_methods = ["GET", "HEAD", "OPTIONS"]
#     cached_methods  = ["GET", "HEAD"]
#
#     # AWS 기본 캐시 최적화 정책 적용, 압축 전송 ON
#     cache_policy_id = data.aws_cloudfront_cache_policy.caching_optimized.id
#     compress        = true
#   }
#
#   # 모든 지역에서 접근 허용
#   restrictions {
#     geo_restriction {
#       restriction_type = "none"
#     }
#   }
#
#   viewer_certificate {
#     cloudfront_default_certificate = local.product_image_cdn_custom_domain_enabled ? false : true
#     acm_certificate_arn            = local.product_image_cdn_custom_domain_enabled ? aws_acm_certificate_validation.product_image_cdn[0].certificate_arn : null
#
#     # HTTPS 연결 시 인증서를 어떤 방식으로 제공할지 결정하는 옵션
#     ssl_support_method       = local.product_image_cdn_custom_domain_enabled ? "sni-only" : null
#     minimum_protocol_version = local.product_image_cdn_custom_domain_enabled ? "TLSv1.2_2021" : "TLSv1"
#   }
#
#   tags = merge(local.common_tags, {
#     Name = "${local.name_prefix}-product-images-cdn"
#   })
# }
#
# # 생성한 CDN이 해당 S3에 접근이 가능하도록 허용해주는 정책
# resource "aws_s3_bucket_policy" "product_images_cloudfront" {
#   bucket = aws_s3_bucket.product_images.id
#
#   policy = jsonencode({
#     Version = "2012-10-17"
#     Statement = [
#       {
#         Sid    = "AllowCloudFrontReadOnly"
#         Effect = "Allow"
#
#         Principal = {
#           Service = "cloudfront.amazonaws.com"
#         }
#
#         Action   = "s3:GetObject"
#         Resource = "${aws_s3_bucket.product_images.arn}/*"
#
#         Condition = {
#           StringEquals = {
#             "AWS:SourceArn" = aws_cloudfront_distribution.product_images.arn
#           }
#         }
#       }
#     ]
#   })
# }
#
# # cdn.hyu1335.cloud -> CloudFront 로 연결하는 A 레코드
# resource "aws_route53_record" "product_image_cdn_alias" {
#   count = local.product_image_cdn_custom_domain_enabled ? 1 : 0
#
#   zone_id = data.aws_route53_zone.public.zone_id
#   name    = var.product_image_cdn_domain
#   type    = "A"
#
#   alias {
#     name                   = aws_cloudfront_distribution.product_images.domain_name
#     zone_id                = aws_cloudfront_distribution.product_images.hosted_zone_id
#     evaluate_target_health = false
#   }
# }