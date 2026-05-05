# customer 서버용 Target Group 생성
resource "aws_lb_target_group" "customer" {
  name        = substr("${local.name_prefix}-customer-tg", 0, 32)
  port        = var.customer_container_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.this.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = var.customer_health_check_path
    protocol            = "HTTP"
    matcher             = "200"
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-customer-tg"
  })
}

# admin ECS Task용 타겟그룹 생성
resource "aws_lb_target_group" "admin" {
  count = var.admin_enabled ? 1 : 0

  name        = substr("${local.name_prefix}-admin-tg", 0, 32)
  port        = var.admin_container_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.this.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = var.admin_health_check_path
    protocol            = "HTTP"
    matcher             = "200"
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-admin-tg"
  })
}

# alarm 서버용 Target Group 생성
resource "aws_lb_target_group" "alarm" {
  name        = substr("${local.name_prefix}-alarm-tg", 0, 32)
  port        = var.alarm_container_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.this.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = var.alarm_health_check_path
    protocol            = "HTTP"
    matcher             = "200"
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-alarm-tg"
  })
}

# chat 서버용 Target Group 생성
resource "aws_lb_target_group" "chat" {
  name        = substr("${local.name_prefix}-chat-tg", 0, 32)
  port        = var.chat_container_port
  protocol    = "HTTP"
  vpc_id      = aws_vpc.this.id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = var.chat_health_check_path
    protocol            = "HTTP"
    matcher             = "200"
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-chat-tg"
  })
}