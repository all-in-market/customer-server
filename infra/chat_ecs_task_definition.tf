resource "aws_ecs_task_definition" "chat" {
  family                   = "${local.name_prefix}-chat"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = tostring(var.chat_ecs_task_cpu)
  memory                   = tostring(var.chat_ecs_task_memory)
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = var.chat_container_name
      image     = local.chat_container_image
      essential = true

      portMappings = [
        {
          containerPort = var.chat_container_port
          hostPort      = var.chat_container_port
          protocol      = "tcp"
        }
      ]

      environment = local.chat_container_environment

      secrets = [
        {
          name      = "DB_PASSWORD"
          valueFrom = aws_ssm_parameter.db_password.arn
        },
        {
          name      = "OPENAI_API_KEY"
          valueFrom = aws_ssm_parameter.openai_api_key.arn
        },
        {
          name      = "DEEPSEEK_API_KEY"
          valueFrom = aws_ssm_parameter.deepseek_api_key.arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.chat.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "chat"
        }
      }
    }
  ])

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-chat-task"
  })
}