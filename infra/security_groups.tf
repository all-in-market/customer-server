resource "aws_security_group" "alb" {
  name        = "${local.name_prefix}-alb-sg"
  description = "Allow HTTP/HTTPS from internet"
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-alb-sg"
  })
}

resource "aws_security_group" "customer_ecs" {
  name        = "${local.name_prefix}-customer-ecs-sg"
  description = "Allow Customer app traffic only from ALB"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "Customer app port from ALB"
    from_port       = var.customer_container_port
    to_port         = var.customer_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-customer-ecs-sg"
  })
}

resource "aws_security_group" "alarm_ecs" {
  name        = "${local.name_prefix}-alarm-ecs-sg"
  description = "Allow Alarm app traffic only from ALB"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "Alarm app port from ALB"
    from_port       = var.alarm_container_port
    to_port         = var.alarm_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-alarm-ecs-sg"
  })
}

resource "aws_security_group" "admin_task" {
  name        = "${local.name_prefix}-admin-task-sg"
  description = "Allow admin app traffic only from ALB"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "Admin app port from ALB"
    from_port       = var.admin_container_port
    to_port         = var.admin_container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-admin-task-sg"
  })
}

resource "aws_security_group" "admin_ec2" {
  name        = "${local.name_prefix}-admin-ec2-sg"
  description = "EC2 host SG (no inbound, outbound only)"
  vpc_id      = aws_vpc.this.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-admin-ec2-sg"
  })
}

resource "aws_security_group" "rds" {
  name        = "${local.name_prefix}-rds-sg"
  description = "Allow PostgreSQL only from ECS and Admin EC2"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "PostgreSQL from Customer ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.customer_ecs.id]
  }

  ingress {
    description     = "PostgreSQL from Alarm ECS"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.alarm_ecs.id]
  }

  ingress {
    description     = "PostgreSQL from Admin Task"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.admin_task.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-rds-sg"
  })
}

resource "aws_security_group" "redis" {
  name        = "${local.name_prefix}-redis-sg"
  description = "Allow Redis only from ECS and Admin EC2"
  vpc_id      = aws_vpc.this.id

  ingress {
    description     = "Redis from Customer ECS"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.customer_ecs.id]
  }

  ingress {
    description     = "Redis from Alarm ECS"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.alarm_ecs.id]
  }

  ingress {
    description     = "Redis from Admin Task"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.admin_task.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(local.common_tags, {
    Name = "${local.name_prefix}-redis-sg"
  })
}
