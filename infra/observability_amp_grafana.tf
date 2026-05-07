# AWS Managed Prometheus 저장소 생성
resource "aws_prometheus_workspace" "app" {
  count = var.managed_prometheus_enabled ? 1 : 0

  alias = "${local.name_prefix}-${var.environment}-amp"

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-${var.environment}-amp"
  })
}

# ADOT가 수집한 Prometheus 메트릭을 AMP로 전송할 수 있게 하는 권한
resource "aws_iam_role_policy_attachment" "ecs_task_amp_remote_write" {
  count = var.managed_prometheus_enabled ? 1 : 0

  role       = aws_iam_role.ecs_task.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonPrometheusRemoteWriteAccess"
}

# AWS Managed Grafana 작업공간 생성
resource "aws_grafana_workspace" "this" {
  count = var.managed_grafana_enabled ? 1 : 0

  name = "${local.name_prefix}-${var.environment}-grafana"

  # 현재 AWS 계정 데이터만 접근 허용
  # aws configure로 등록했던 IAM의 AWS 계정
  account_access_type = "CURRENT_ACCOUNT"

  # 생성된 Grafana 전용 URL에 접속하여 AWS 로그인을 해야 대시보드 접근 가능
  authentication_providers = ["AWS_SSO"]

  # Grafana 권한을 AWS가 자동 관리
  permission_type = "SERVICE_MANAGED"

  role_arn = aws_iam_role.grafana_workspace[0].arn

  # Grafana가 CloudWatch/Prometheus를 조회할 수 있게 하는 설정
  data_sources = [
    "PROMETHEUS",
    "CLOUDWATCH"
  ]

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-${var.environment}-grafana"
  })
}

resource "aws_iam_role" "grafana_workspace" {
  count = var.managed_grafana_enabled ? 1 : 0

  name = "${local.name_prefix}-${var.environment}-grafana-workspace-role"

  # AWS Managed Grafana 서비스만 해당 Role AssumeRole 가능
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "grafana.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

# Grafana Workspace가 Cloudwatch를 조회할 권한
resource "aws_iam_role_policy_attachment" "grafana_cloudwatch" {
  count      = var.managed_grafana_enabled ? 1 : 0
  role       = aws_iam_role.grafana_workspace[0].name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchReadOnlyAccess"
}

# Grafana Workspace가 AWS Managed Prometheus(AMP) 에서 쿼리를 통해 조회할 권한
resource "aws_iam_role_policy_attachment" "grafana_prometheus" {
  count      = var.managed_grafana_enabled ? 1 : 0
  role       = aws_iam_role.grafana_workspace[0].name
  policy_arn = "arn:aws:iam::aws:policy/AmazonPrometheusQueryAccess"
}