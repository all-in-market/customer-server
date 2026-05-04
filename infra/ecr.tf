# customer 서버용 ECR 생성
resource "aws_ecr_repository" "customer" {
  name                 = local.customer_ecr_repository_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = merge(local.common_tags, {
    Name = local.customer_ecr_repository_name
  })
}

resource "aws_ecr_lifecycle_policy" "customer" {
  repository = aws_ecr_repository.customer.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# Admin 서버용 ECR 생성
resource "aws_ecr_repository" "admin" {
  name                 = local.admin_ecr_repository_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = merge(local.common_tags, {
    Name = local.admin_ecr_repository_name
  })
}

resource "aws_ecr_lifecycle_policy" "admin" {
  repository = aws_ecr_repository.admin.name

  # 최근 10개 이미지까지만 저장
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 admin images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

# Alarm 서버용 ECR 생성
resource "aws_ecr_repository" "alarm" {
  name                 = local.alarm_ecr_repository_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = merge(local.common_tags, {
    Name = local.alarm_ecr_repository_name
  })
}

resource "aws_ecr_lifecycle_policy" "alarm" {
  repository = aws_ecr_repository.alarm.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep last 10 alarm images"
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = 10
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}