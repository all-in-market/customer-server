resource "aws_ecs_service" "customer" {
  name                              = "${local.name_prefix}-customer-service"
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.customer.arn
  desired_count                     = var.customer_ecs_desired_count
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 120
  force_new_deployment              = true

  # prod 환경에서는 무중단 배포 가능하도록 설정
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  network_configuration {
    subnets          = [for subnet in aws_subnet.ecs_private : subnet.id]
    security_groups  = [aws_security_group.customer_ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.customer.arn
    container_name   = var.customer_container_name
    container_port   = var.customer_container_port
  }

  lifecycle {
    ignore_changes = [
      desired_count
    ]
  }

  depends_on = [
    aws_lb_listener.https,
    aws_iam_role_policy_attachment.ecs_task_execution_default,
    aws_iam_role_policy_attachment.ecs_task_execution_ssm
  ]

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-customer-service"
  })
}
