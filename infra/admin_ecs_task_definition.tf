# 관리자 서버 컨테이너 실행 정의
# 기존 EC2 user_data의 docker run 역할을 ECS Task Definition이 대체
resource "aws_ecs_task_definition" "admin" {
  count = var.admin_enabled ? 1 : 0

  family                   = "${local.name_prefix}-admin"
  network_mode             = "awsvpc"
  requires_compatibilities = ["EC2"]
  cpu                      = tostring(var.admin_ecs_task_cpu)
  memory                   = tostring(var.admin_ecs_task_memory)
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name      = var.admin_container_name
      image     = local.admin_container_image
      essential = true

      portMappings = [
        {
          containerPort = var.admin_container_port
          hostPort      = var.admin_container_port
          protocol      = "tcp"
        }
      ]

      environment = local.admin_container_environment

      secrets = [
        {
          name      = "DB_PASSWORD"
          valueFrom = aws_ssm_parameter.db_password.arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.admin.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "admin"
        }
      }
    }
  ])

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-admin-task"
  })
}
