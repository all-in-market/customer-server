resource "aws_ecs_task_definition" "customer" {
  family                   = "${local.name_prefix}-customer"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = tostring(var.customer_ecs_task_cpu)
  memory                   = tostring(var.customer_ecs_task_memory)
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  # managed_prometheus_enabled = ture 일 경우, customer app과 ADOT collector 둘 다 실행
  # managed_prometheus_enabled = false 일 경우, customer app만 실행
  container_definitions = jsonencode(
    concat(
      [
        {
          name      = var.customer_container_name
          image     = local.customer_container_image
          essential = true

          portMappings = [
            {
              containerPort = var.customer_container_port
              hostPort      = var.customer_container_port
              protocol      = "tcp"
            }
          ]

          environment = local.customer_container_environment

          secrets = [
            {
              name      = "DB_PASSWORD"
              valueFrom = aws_ssm_parameter.db_password.arn
            },
            {
              name      = "SERVER_SECRET_KEY"
              valueFrom = aws_ssm_parameter.server_secret_key.arn
            }
          ]

          logConfiguration = {
            logDriver = "awslogs"
            options = {
              awslogs-group         = aws_cloudwatch_log_group.customer.name
              awslogs-region        = var.aws_region
              awslogs-stream-prefix = "customer"
            }
          }
        }
      ],
      var.managed_prometheus_enabled ? [local.customer_adot_container] : []
    )
  )


  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-customer-task"
  })
}
