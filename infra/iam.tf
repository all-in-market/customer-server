data "aws_iam_policy_document" "ecs_task_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ecs_task_execution" {
  name               = "${local.name_prefix}-ecsTaskExecutionRole"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_default" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_task" {
  name               = "${local.name_prefix}-ecsTaskRole"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume_role.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "ecs_task_optional" {
  for_each   = toset(var.task_role_policy_arns)
  role       = aws_iam_role.ecs_task.name
  policy_arn = each.value
}

data "aws_iam_policy_document" "ecs_task_execution_ssm" {

  # aws_ssm_parameter.db_password.arn에 있는 데이터를 읽어올 권한 부여
  statement {
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters"
    ]

    resources = [
      aws_ssm_parameter.db_password.arn,
      aws_ssm_parameter.openai_api_key.arn,
      aws_ssm_parameter.deepseek_api_key.arn
    ]
  }

  # kms 키로 복호화할 권한 부여
  statement {
    actions = [
      "kms:Decrypt",
      "kms:DescribeKey"
    ]

    resources = [
      aws_kms_key.ssm.arn
    ]
    condition {
      test     = "StringEquals"
      variable = "kms:ViaService"
      values   = ["ssm.${data.aws_region.current.name}.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "kms:EncryptionContext:PARAMETER_ARN"
      values   = [
        aws_ssm_parameter.db_password.arn,
        aws_ssm_parameter.openai_api_key.arn,
        aws_ssm_parameter.deepseek_api_key.arn
      ]
    }
  }
}

resource "aws_iam_policy" "ecs_task_execution_ssm" {
  name   = "${local.name_prefix}-ecs-task-execution-ssm-policy"
  policy = data.aws_iam_policy_document.ecs_task_execution_ssm.json

  tags = local.common_tags
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_ssm" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = aws_iam_policy.ecs_task_execution_ssm.arn
}