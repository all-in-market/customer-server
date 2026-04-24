data "aws_iam_policy_document" "github_actions_assume_role" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    # oidc provider 에서 온 요청만 허용
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github.arn]
    }

    # token.actions.githubusercontent.com 는 Github Actions 에서 발급한 oidc 토큰을 의미
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values = [
        "repo:${var.github_owner}/${var.github_repo}:ref:refs/heads/${var.github_branch}"
      ]
    }
  }
}

# Github Actions 가 사용할 IAM Role 틀 생성
# 위 github_actions_assume_role 조건을 만족하는 경우에만 해당 Role 사용 가능
resource "aws_iam_role" "github_actions" {
  name               = "${local.name_prefix}-github-actions-role"
  assume_role_policy = data.aws_iam_policy_document.github_actions_assume_role.json

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-github-actions-role"
  })
}

# 위 github_actions IAM Role에 부여할 권한을 정의
data "aws_iam_policy_document" "github_actions_deploy" {

  # ECR 로그인 권한
  statement {
    sid = "EcrLogin"
    actions = [
      "ecr:GetAuthorizationToken"
    ]
    resources = ["*"]
  }

  # ECR push 권한
  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
      "ecr:BatchGetImage",
      "ecr:DescribeRepositories",
      "ecr:DescribeImages"
    ]
    resources = [
      aws_ecr_repository.app.arn
    ]
  }

  # ECS 배포 권한
  statement {
    sid = "EcsDeploy"
    actions = [
      "ecs:DescribeClusters",
      "ecs:DescribeServices",
      "ecs:DescribeTaskDefinition",
      "ecs:RegisterTaskDefinition",
      "ecs:UpdateService"
    ]
    resources = ["*"]
  }

  # ECS Task Definition 등록 시 필요한 권한
  # Task Definition 생성 시 태스크 역할, 태스크 실행 역할 등록해야 함
  # 미리 만들어 둔 태스크 역할, 태스크 실행 역할을 전달하기 위한 권한
  statement {
    sid = "PassEcsRoles"
    actions = [
      "iam:PassRole"
    ]
    resources = [
      aws_iam_role.ecs_task_execution.arn,
      aws_iam_role.ecs_task.arn
    ]
  }
}

# 위에서 정의한 권한을 가지고 IAM Role 에 붙일 policy 생성
resource "aws_iam_policy" "github_actions_deploy" {
  name   = "${local.name_prefix}-github-actions-deploy-policy"
  policy = data.aws_iam_policy_document.github_actions_deploy.json

  tags = local.common_tags
}

# 생성한 policy를 IAM Role 에 붙임
# Role 안에서 직접 연결해도 되지만 이러한 방식으로 Policy 재사용 가능하게 할 수 있음
resource "aws_iam_role_policy_attachment" "github_actions_deploy" {
  role       = aws_iam_role.github_actions.name
  policy_arn = aws_iam_policy.github_actions_deploy.arn
}