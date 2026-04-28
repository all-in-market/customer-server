resource "aws_ecs_service" "this" {
  name                              = "${local.name_prefix}-service"
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.this.arn
  desired_count                     = local.ecs.desired_count
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 120
  force_new_deployment              = true

  network_configuration {
    subnets          = [for subnet in aws_subnet.ecs_private : subnet.id]
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = var.container_name
    container_port   = var.container_port
  }

  depends_on = [
    aws_lb_listener.http,
    aws_iam_role_policy_attachment.ecs_task_execution_default
  ]

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-service"
  })
}
