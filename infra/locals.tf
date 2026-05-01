locals {

  # =========================
  # 생성 전에 필요한 값
  # =========================

  common_tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }

  name_prefix = var.project_name

  customer_ecr_repository_name = coalesce(var.customer_ecr_repository_name, "${var.project_name}-customer")
  admin_ecr_repository_name = coalesce(var.admin_ecr_repository_name, "${var.project_name}-admin")

  customer_log_group_name = "/ecs/${var.project_name}/customer"
  admin_log_group_name = "/ecs/${var.project_name}/admin"

  admin_ec2_subnet_ids = var.environment == "prod" ? [for subnet in aws_subnet.ecs_private : subnet.id] : [aws_subnet.ecs_private["0"].id]

  rds = {
    multi_az = var.environment == "prod"
    deletion_protection = var.environment == "prod"
    skip_final_snapshot = var.environment != "prod"
  }

  redis = {
    num_cache_clusters         = var.environment == "prod" ? 2 : 1
    automatic_failover_enabled = var.environment == "prod"
    multi_az_enabled           = var.environment == "prod"
  }
}

locals {

  # =========================
  # 생성 후에 알 수 있는 값
  # =========================

  customer_container_image  = "${aws_ecr_repository.customer.repository_url}:${var.customer_image_tag}"
  admin_container_image     = "${aws_ecr_repository.admin.repository_url}:${var.admin_image_tag}"


  customer_environment_variables = merge(
    var.customer_environment_variables,
    {
      SPRING_PROFILES_ACTIVE = var.environment == "prod" ? "prod" : "dev"

      DDL_AUTO   = "validate"
      SPRING_JPA_SHOW_SQL = "false"
      HIBERNATE_FORMAT_SQL = "false"

      DB_HOST    = aws_db_instance.this.address
      DB_PORT    = "5432"
      DB_NAME    = var.db_name
      DB_USERNAME = var.db_username
      REDIS_HOST = aws_elasticache_replication_group.this.primary_endpoint_address
      REDIS_PORT = "6379"
    }
  )

  admin_environment_variables = merge(
    var.admin_environment_variables,
    {
      SPRING_PROFILES_ACTIVE = var.environment == "prod" ? "prod" : "dev"

      DDL_AUTO             = "validate"
      SPRING_JPA_SHOW_SQL  = "false"
      HIBERNATE_FORMAT_SQL = "false"

      DB_HOST     = aws_db_instance.this.address
      DB_PORT     = "5432"
      DB_NAME     = var.db_name
      DB_USERNAME = var.db_username

      REDIS_HOST = aws_elasticache_replication_group.this.primary_endpoint_address
      REDIS_PORT = "6379"
    }
  )

  customer_container_environment = [
    for key, value in local.customer_environment_variables : {
      name  = key
      value = value
    }
  ]

  admin_container_environment = [
    for key, value in local.admin_environment_variables : {
      name  = key
      value = value
    }
  ]
}
