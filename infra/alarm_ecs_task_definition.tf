resource "aws_ecs_task_definition" "alarm" {
  family                   = "${local.name_prefix}-alarm"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = tostring(var.alarm_ecs_task_cpu)
  memory                   = tostring(var.alarm_ecs_task_memory)
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = var.alarm_container_name
      image     = local.alarm_container_image
      essential = true

      portMappings = [
        {
          containerPort = var.alarm_container_port
          hostPort      = var.alarm_container_port
          protocol      = "tcp"
        }
      ]

      environment = local.alarm_container_environment

      secrets = [
        {
          name      = "DB_PASSWORD"
          valueFrom = aws_ssm_parameter.db_password.arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.alarm.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "alarm"
        }
      }
    }
  ])

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-alarm-task"
  })
}