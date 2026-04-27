resource "aws_iam_openid_connect_provider" "github" {

  # GitHub OIDC 토큰 발급 서버에서 오는 토큰만 신뢰
  url = "https://token.actions.githubusercontent.com"

  client_id_list = [
    "sts.amazonaws.com"
  ]

  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1"
  ]

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-github-oidc"
  })
}