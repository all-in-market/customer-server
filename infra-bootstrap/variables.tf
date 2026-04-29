variable "aws_region" {
  type    = string
  default = "ap-northeast-2"
}

variable "project_name" {
  type    = string
  default = "all-in-market"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "route53_zone_name" {
  description = "Public Route53 hosted zone name, for example example.com."
  type        = string

  validation {
    condition     = length(trimspace(var.route53_zone_name)) > 0
    error_message = "route53_zone_name은 필수입니다. 예: example.com"
  }
}