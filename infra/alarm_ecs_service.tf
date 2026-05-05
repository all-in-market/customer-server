resource "aws_ecs_service" "alarm" {
  name                              = "${local.name_prefix}-alarm-service"
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.alarm.arn
  desired_count                     = var.alarm_ecs_desired_count
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 120
  force_new_deployment              = true

  deployment_minimum_healthy_percent = var.environment == "prod" ? 100 : 0
  deployment_maximum_percent         = var.environment == "prod" ? 200 : 100

  network_configuration {
    subnets          = [for subnet in aws_subnet.ecs_private : subnet.id]
    security_groups  = [aws_security_group.alarm_ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.alarm.arn
    container_name   = var.alarm_container_name
    container_port   = var.alarm_container_port
  }

  lifecycle {
    ignore_changes = [
      desired_count,
      task_definition
    ]
  }

  depends_on = [
    aws_lb_listener.https,
    aws_iam_role_policy_attachment.ecs_task_execution_default,
    aws_iam_role_policy_attachment.ecs_task_execution_ssm
  ]

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-alarm-service"
  })
}