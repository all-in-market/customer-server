# 관리자 서버 ECS Service
# ECS가 관리자용 EC2 위에서 Task를 실행하고
# 컨테이너의 ip 주소를 Admin용 Target Group에 자동 등록한다.
resource "aws_ecs_service" "admin" {
  count = var.admin_enabled ? 1 : 0

  name                              = "${local.name_prefix}-admin-service"
  cluster                           = aws_ecs_cluster.this.id
  task_definition                   = aws_ecs_task_definition.admin.arn
  desired_count                     = var.admin_ecs_desired_count
  launch_type                       = "EC2"
  health_check_grace_period_seconds = 120
  force_new_deployment              = true

  network_configuration {
    subnets          = local.admin_ec2_subnet_ids
    security_groups  = [aws_security_group.admin_task.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.admin.arn
    container_name   = var.admin_container_name
    container_port   = var.admin_container_port
  }

  depends_on = [
    aws_lb_listener.https,
    aws_instance.admin,
    aws_iam_role_policy_attachment.admin_ec2_ecs_container_instance,
    aws_iam_role_policy_attachment.ecs_task_execution_default,
    aws_iam_role_policy_attachment.ecs_task_execution_ssm
  ]

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-admin-service"
  })
}
