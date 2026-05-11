provider "aws" {
  region = var.aws_region
}

# CloudFront에 연결할 ACM 인증서는
# 무조건 us-east-1에 생성해야 함
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}
